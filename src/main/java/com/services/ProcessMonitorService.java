package com.services;

import com.entities.ProcessMonitor;
import com.repository.ProcessMonitorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessMonitorService {

    @Autowired
    private ProcessMonitorRepository repository;

    @Transactional
    public ProcessMonitor saveOrUpdate(ProcessMonitor processMonitor) {
        ProcessMonitor existing = repository.findByProcessAndTenant(processMonitor.getProcess(), processMonitor.getTenant()).orElse(null);

        if (existing != null) {
            processMonitor.setId(existing.getId());
        }
        return repository.save(processMonitor);
    }
}
