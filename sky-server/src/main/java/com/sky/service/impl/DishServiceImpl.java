package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和口味数据
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //1.向菜品表插入一条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        //2.向口味表插入多条数据
        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        Long total = page.getTotal();
        List<DishVO> dishes= page.getResult();

        return new PageResult(total, dishes);
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(@RequestParam List<Long> ids) {
        //判断当前菜品是否可以被删除--是否处于起售状态
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE)
                //菜品处于起售状态，抛出异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        //判断当前菜品是否可以被删除--是否与套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() != 0){
            //当前菜品被套餐关联了，不能被删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中菜品数据
        //delete from dish where id in ()
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //根据菜品id集合批量删除菜品
        dishMapper.deleteByIds(ids);
        //根据菜品id集合批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    public DishVO getById(Long id) {

        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        //根据菜品id数据查询关联的口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //将查询到的数据封装到dishVo
        DishVO dishVo = new DishVO();
        BeanUtils.copyProperties(dish, dishVo);
        dishVo.setFlavors(dishFlavors);//BeanUtils.copyProperties只能对单个实体类进行复制，不能对封装多个实体类的list集合进行复制

        return dishVo;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //修改菜品基本信息
        dishMapper.update(dish);

        //删除原有的口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 修改售卖状态
     * @param status
     */
    public void updateStatus(Integer status, Long id) {
        dishMapper.updateStatus(status, id);
    }

    /**
     *根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> getByCategoryId(Long categoryId) {
        List<Dish> dishes= dishMapper.getByCategoryId(categoryId);

        return dishes;
    }
}
