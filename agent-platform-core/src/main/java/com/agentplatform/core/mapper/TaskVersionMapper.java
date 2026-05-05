package com.agentplatform.core.mapper;

import com.agentplatform.core.entity.TaskVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 任务版本数据访问层
 */
@Mapper
public interface TaskVersionMapper extends BaseMapper<TaskVersion> {

    /**
     * 获取任务的最大版本号
     */
    @Select("SELECT COALESCE(MAX(version), 0) FROM task_version WHERE task_id = #{taskId}")
    int getMaxVersion(@Param("taskId") Long taskId);
}
