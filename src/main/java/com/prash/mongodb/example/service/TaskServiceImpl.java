package com.prash.mongodb.example.service;

import com.prash.mongodb.example.collection.Task;
import com.prash.mongodb.example.exception.TaskAlreadyExistsException;
import com.prash.mongodb.example.exception.TaskNotFoundException;
import com.prash.mongodb.example.repository.TaskRepository;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.sql.SQLOutput;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;

    /**
     *  Method to create a new task
     *  Before insert, validates if the task already exists
     * @param task - input
     * @return task object as output
     */
    @Override
    public Task createTask(Task task) {

        Optional<Task> optionalTask = taskRepository.findByTaskId(task.getTaskId());
        if (optionalTask.isPresent()) {
            throw new TaskAlreadyExistsException(String.format("Task [%s] already Exists." , task.getTaskId()));
        }
        //task.setTaskId(UUID.randomUUID().toString().split("-")[0]);
        task = taskRepository.save(task);
        return task;
    }

    /**
     * Method to fetch task based on taskId
     * Only the first request fetches the data from the database
     * Fetched record will be saved into Redis cache. Subsequent calls for the same taskId
     * will be fetched from Redis cache
     * @param taskId - input
     * @return task object
     */
    @Override
    @Cacheable(value = "Task")
    public Optional<Task> findTaskById(String taskId) {
        LOG.info("Fetching the data from the database.");
        return taskRepository.findByTaskId(taskId);
    }

    /**
     *  Method to update the existing task
     *  Before update, validates if the task exists
     *  Since CachePut is enabled, When we get a request to update the task
     *  the entry in the cache will also be updated
     * @param task - input
     * @return task object as the out
     */
    @Override
    @CachePut(value = "Task")
    public Task updateTask(Task task) {
        final String taskId = task.getTaskId();
        Task existingTask = taskRepository.findByTaskId(taskId).orElseThrow(
                () -> new TaskNotFoundException(String.format("Task [%s] not found." , taskId))
        );

        existingTask.setTaskType(task.getTaskType());
        existingTask.setTaskId(task.getTaskId());
        existingTask.setAssignee(task.getAssignee());
        existingTask.setSeverity(task.getSeverity());
        existingTask.setDescription(task.getDescription());
        task = taskRepository.save(task);
        return task;
    }

    /**
     *  Method to delete the existing Task
     *  Before update, validates if the task exists
     *  Since CacheEvict is enabled, On every delete request
     *  cached entry with the taskId will also be deleted from the cache
     * @param taskId - input
     * @return task object as the out
     */
    @Override
    @Caching(
            evict = {@CacheEvict(value = "Task"), @CacheEvict(value = "Tasks")}
    )
    //@CacheEvict(cacheNames = "task", allEntries = true) To clear the cache
    public Task deleteTask(String taskId) {
        Task task = taskRepository.findByTaskId(taskId).orElseThrow(
                () -> new TaskNotFoundException(String.format("Task [%s] not found." , taskId))
        );
        taskRepository.deleteById(taskId);
        return task;
    }


    /**
     * Method to fetch all the tasks from database
     * @return List of tasks
     */
    @Override
    @Cacheable(value = "Tasks")
    public List<Task> findAllTasks() {
        LOG.info("Fetching all the data from the database.");
        return taskRepository.findAll();
    }


    /**
     * Method to fetch all tasks based on severity
     * @param severity - input
     * @return List of tasks
     */
    @Override
    public List<Task> findTasksBySeverity(String severity) {
        return taskRepository.findBySeverity(severity);
    }

    /**
     * Method to fetch all tasks based on severity and assignee using custom query
     * @param severity - input
     * @param assignee - input
     * @return List of tasks
     */
    @Override
    public List<Task>  findTasksBySeverityAndAssignee(String severity, String assignee) {
        return taskRepository.findBySeverityAndAssignee(severity, assignee);
    }

}
