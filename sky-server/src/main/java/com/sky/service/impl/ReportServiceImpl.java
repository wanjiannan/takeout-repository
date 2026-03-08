package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间营业额的数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //计算日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate data : dateList) {
            //查询date日期对应的营业额数据，营业额是指：状态为”已完成"的订单金额合计

            LocalDateTime beginTime = LocalDateTime.of(data, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(data, LocalTime.MAX);

            //select sum(amount) from orders where order_time > and order_time < ? and status = 5
            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover=orderMapper.sumByMap(map);

            turnover = turnover == null ? 0.0 : turnover ;//防止数据为空

            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 统计指定时间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放begin到end 之间的每天对应的数据
        List<LocalDate> dateList=new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();//存放每天的新增用户数量

        List<Integer> totalUserList = new ArrayList<>();//存放总用户数量


        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();

            //先查询每天的总用户数量
            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);

            //再查询每天的新增用户数量
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);

        }

        //封装结果数据
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 统计订单情况
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList= new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> ordersList=new ArrayList<>();

        List<Integer> completedOrdersList = new ArrayList<>();

        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select count(id) where begin &gt; ? and end &lt; ?

            Integer ordersNumber = getOrderCount(beginTime,endTime,null);

            Integer completedOrdersNumber = getOrderCount(beginTime,endTime,Orders.COMPLETED);

            ordersList.add(ordersNumber);

            completedOrdersList.add(completedOrdersNumber);

        }

        //计算总的订单数量
        Integer totalOrders = ordersList.stream().reduce(Integer::sum).get();

        //计算总的有效订单数量
        Integer totalCompletedOrders = completedOrdersList.stream().reduce(Integer::sum).get();

        //订单完成率

        double orderCompletedRate=0.0;
        if (totalOrders!=0) {
             orderCompletedRate = totalCompletedOrders.doubleValue() / totalOrders;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(ordersList,","))
                .validOrderCountList(StringUtils.join(completedOrdersList,","))
                .totalOrderCount(totalOrders)
                .validOrderCount(totalCompletedOrders)
                .orderCompletionRate(orderCompletedRate)
                .build();

    }

    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);

        return orderMapper.countByMap(map);
    }


    /**
     * 销量前10统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10= orderMapper.getSalesTop10(beginTime,endTime);

        //将List<GoodsSalesDTO>转换成SalesTop10ReportVO格式
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> sales = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String saleList = StringUtils.join(sales, ",");

        //封装返回结果数据
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(saleList)
                .build();
    }
}
