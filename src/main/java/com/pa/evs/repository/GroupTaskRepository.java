package com.pa.evs.repository;

import com.pa.evs.model.Group;
import com.pa.evs.model.GroupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Repository
public interface GroupTaskRepository extends JpaRepository<GroupTask, Long> {
    List<GroupTask> findByGroupId(Long groupId);
}
