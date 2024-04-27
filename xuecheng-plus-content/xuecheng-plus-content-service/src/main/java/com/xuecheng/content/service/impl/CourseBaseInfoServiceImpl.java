package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    final
    CourseBaseMapper courseBaseMapper;

    final
    CourseMarketMapper courseMarketMapper;

    final
    CourseCategoryMapper courseCategoryMapper;

    public CourseBaseInfoServiceImpl(CourseBaseMapper courseBaseMapper, CourseMarketMapper courseMarketMapper, CourseCategoryMapper courseCategoryMapper) {
        this.courseBaseMapper = courseBaseMapper;
        this.courseMarketMapper = courseMarketMapper;
        this.courseCategoryMapper = courseCategoryMapper;
    }

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {
        //拼装查询条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称模糊查询.在sql中拼接course_base.name like '%值%'
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
        //根据课程审核状态查询 course_base.audit_status= ?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());
        //根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),CourseBase::getStatus,courseParamsDto.getPublishStatus());
        //创建page分页参数对象，参数：当前页码，每页记录数。
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        //开始进行分页查询
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();
        //List<T> items,long counts,long page,long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<CourseBase>(items,total,pageParams.getPageNo(),pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {

        //参数的合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            throw new XueChengPlusException("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
//        }
        //向课程基本信息表course_base添加数据
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(dto,courseBase);
        courseBase.setCompanyId(companyId);
        courseBase.setCreateDate(LocalDateTime.now());
        courseBase.setStatus("203001");
        courseBase.setAuditStatus("202002");;
        int insert = courseBaseMapper.insert(courseBase);
        if(insert <= 0){
            throw new RuntimeException("添加课程失败");
        }
        //向课程营销表course_market添加数据
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);
        Long courseId = courseBase.getId();
        courseMarket.setId(courseId);
        //保存营销信息
        saveCourseMarket(courseMarket);
        //从数据库查询课程详细信息
        return getCourseBaseInfo(courseId);
    }

    public  CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }

        CourseCategory courseCategory1 = courseCategoryMapper.selectById(courseBase.getMt());
        CourseCategory courseCategory2 = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setMtName(courseCategory1.getName());
        courseBaseInfoDto.setStName(courseCategory2.getName());
        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        Long courseId = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            XueChengPlusException.cast("课程不存在!");
        }
        //数据合法性校验
        if(!companyId.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }
        //封装数据
        BeanUtils.copyProperties(editCourseDto,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());

        //更新数据库
        int i = courseBaseMapper.updateById(courseBase);
        if(i <= 0){
            XueChengPlusException.cast("修改课程失败");
        }
        //更新营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto,courseMarketNew);
        if("201000".equals(editCourseDto.getCharge())){
            courseMarketNew.setPrice(0.0F);
            courseMarketNew.setOriginalPrice(0.0F);
        }
        int j = courseMarketMapper.updateById(courseMarketNew);
        if(j <= 0){
            XueChengPlusException.cast("修改课程营销信息失败");
        }
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    private int saveCourseMarket(CourseMarket courseMarketNew){
        String charge = courseMarketNew.getCharge();
        if(StringUtils.isEmpty(charge)){
            throw new RuntimeException("收费规则没有选择");
        }
        if("201001".equals(charge)){
            if(courseMarketNew.getPrice() == null  || courseMarketNew.getPrice().floatValue() <= 0){
                throw new XueChengPlusException("课程为收费价格不能为空且必须大于0");
            }
        }

        CourseMarket courseMarket = courseMarketMapper.selectById(courseMarketNew.getId());
        if(courseMarket == null){
            return courseMarketMapper.insert(courseMarketNew);
        }else {
            BeanUtils.copyProperties(courseMarketNew,courseMarket);
            courseMarket.setId(courseMarketNew.getId());
            return courseMarketMapper.updateById(courseMarket);
        }
    }
}
