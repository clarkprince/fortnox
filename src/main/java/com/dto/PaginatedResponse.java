package com.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
public class PaginatedResponse<T> {
    private List<T> data;
    private int page;
    private int pageSize;
    private long records;
    private long recordsTotal;
    private int totalPages;

    public static <T> PaginatedResponse<T> from(Page<T> page) {
        PaginatedResponse<T> response = new PaginatedResponse<>();
        response.setData(page.getContent());
        response.setPage(page.getNumber() + 1); // Add 1 to convert from 0-based to 1-based
        response.setPageSize(page.getSize());
        response.setRecords(page.getNumberOfElements());
        response.setRecordsTotal(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}
