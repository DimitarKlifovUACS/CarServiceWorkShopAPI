package com.example.CarService.service;

import com.example.CarService.dto.response.RevenueReportResponse;
import com.example.CarService.repository.ServiceOrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ServiceOrderRepository serviceOrderRepository;

    public ReportService(ServiceOrderRepository serviceOrderRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
    }

    public List<RevenueReportResponse> getRevenueByMonth() {
        List<Object[]> results = serviceOrderRepository.revenueByMonth();
        List<RevenueReportResponse> report = new ArrayList<>();

        for (Object[] row : results) {
            RevenueReportResponse entry = new RevenueReportResponse();
            entry.setMonth(((Number) row[0]).intValue());
            entry.setYear(((Number) row[1]).intValue());
            entry.setTotalRevenue(((Number) row[2]).doubleValue());
            report.add(entry);
        }

        return report;
    }
}
