package com.interview.prep.repository;

import com.interview.prep.model.Task;
import com.interview.prep.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}
