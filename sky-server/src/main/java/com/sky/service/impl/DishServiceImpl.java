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
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 新增菜品口味
     * @param dishDTO
     */
    @Transactional //设计到多个表的操作，要加个事务注解，保证方法要么全成功要么全失败
    public void     saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取insert语句产生的主键值mapper里面的useGeneratedKeys="true" keyProperty="id"
        Long dishId=dish.getId();


        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors !=null && flavors.size()>0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品的批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除---是否存在起售的菜品？
        for(Long id:ids){
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                //当前菜品处于起售状态，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前菜品是否能够删除---是否被套餐关联了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            //当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表的菜品数据
        /*for (Long id : ids) {
            dishMapper.deleteById(id);
            //删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }*/

        //根据菜品id集合来删除菜品表的菜品数据
        dishMapper.deleteByIds(ids);

        //根据菜品id集合来删除关联的口味表数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据菜品来查询菜品和对应的口味数据
     * @param id
     * @return
     */
    public DishVO getDishByIdWithFlavor(Long id) {
        //根据id来查询菜品数据
        Dish dish=dishMapper.getById(id);

        //根据菜品来查询口味数据
        List<DishFlavor> dishFlavors=dishFlavorMapper.getByDishId(id);

        //讲查询到的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //修改菜品表基本信息
        dishMapper.update(dish);

        //删除菜品的口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //新增菜品的口味信息
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors !=null && flavors.size()>0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }


}
