package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据多个菜品id查询套餐id
     */
    public List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 插入套餐和菜品关系的数据
     * @param setmealDishes
     */
    @AutoFill(OperationType.INSERT)
    void insert(List<SetmealDish> setmealDishes);
}
