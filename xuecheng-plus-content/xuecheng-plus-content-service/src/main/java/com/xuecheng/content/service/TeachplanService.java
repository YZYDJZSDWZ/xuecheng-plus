package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

//课程计划管理相关接口
public interface TeachplanService {
    //根据课程id查询课程计划
    List<TeachplanDto> findTeachplanTree(Long courseId);

    void saveTeachplan(SaveTeachplanDto saveTeachplanDto);
}
