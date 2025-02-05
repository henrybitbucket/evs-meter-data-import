package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.GroupTask;

@Transactional
@Repository
public interface GroupTaskRepository extends JpaRepository<GroupTask, Long> {
    List<GroupTask> findByGroupId(Long groupId);
    
    @Modifying
    @Query(value = "DELETE FROM LogBatchGroupTask WHERE task.id = ?1")
    void deleteTaskLog(Long groupId);
}
