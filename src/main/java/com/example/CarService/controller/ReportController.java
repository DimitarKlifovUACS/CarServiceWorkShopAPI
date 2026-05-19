package com.example.CarService.controller;

import com.example.CarService.dto.response.RevenueReportResponse;
import com.example.CarService.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<RevenueReportResponse>> getRevenueByMonth(){
        return ResponseEntity.ok(reportService.getRevenueByMonth());
    }
}
