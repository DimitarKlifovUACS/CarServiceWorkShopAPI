package com.example.CarService;

import com.example.CarService.entity.*;
import com.example.CarService.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

    @Component
    public class DataSeeder implements CommandLineRunner {

        private final VehicleRepository vehicleRepository;
        private final ServiceOrderRepository serviceOrderRepository;
        private final WorkItemRepository workItemRepository;
        private final PartRepository partRepository;

        public DataSeeder(VehicleRepository vehicleRepository,
                          ServiceOrderRepository serviceOrderRepository,
                          WorkItemRepository workItemRepository,
                          PartRepository partRepository) {
            this.vehicleRepository = vehicleRepository;
            this.serviceOrderRepository = serviceOrderRepository;
            this.workItemRepository = workItemRepository;
            this.partRepository = partRepository;
        }

        @Override
        public void run(String... args) {

            // ── Vehicles ──────────────────────────────────────────────
            Vehicle v1 = new Vehicle();
            v1.setLicensePlate("ABC-123");
            v1.setOwnerName("Dimitar Klifov");
            v1.setMake("BMW");
            v1.setModel("320i");
            v1.setYear(2020);
            vehicleRepository.save(v1);

            Vehicle v2 = new Vehicle();
            v2.setLicensePlate("DEF-456");
            v2.setOwnerName("Maja Klifova");
            v2.setMake("Suzuki");
            v2.setModel("S-Cross");
            v2.setYear(2024);
            vehicleRepository.save(v2);

            Vehicle v3 = new Vehicle();
            v3.setLicensePlate("GHI-789");
            v3.setOwnerName("Vladimir Klifov");
            v3.setMake("Abarth");
            v3.setModel("595 Competizione");
            v3.setYear(2017);
            vehicleRepository.save(v3);

            // ── Completed Order 1 (v1 - 2 months ago) ─────────────────
            ServiceOrder order1 = new ServiceOrder();
            order1.setVehicle(v1);
            order1.setStatus("COMPLETED");
            order1.setCreatedAt(LocalDateTime.now().minusMonths(2).minusDays(3));
            order1.setCompletedAt(LocalDateTime.now().minusMonths(2));
            serviceOrderRepository.save(order1);

            WorkItem wi1 = new WorkItem();
            wi1.setOrder(order1);
            wi1.setDescription("Replace front brake pads");
            wi1.setEstimatedHours(1.5);
            wi1.setMechanicName("Stefan");
            wi1.setStatus("DONE");
            workItemRepository.save(wi1);

            WorkItem wi2 = new WorkItem();
            wi2.setOrder(order1);
            wi2.setDescription("Oil change");
            wi2.setEstimatedHours(0.5);
            wi2.setMechanicName("Stefan");
            wi2.setStatus("DONE");
            workItemRepository.save(wi2);

            Part p1 = new Part();
            p1.setOrder(order1);
            p1.setName("Brake Pads");
            p1.setQuantity(2);
            p1.setUnitPrice(45.00);
            partRepository.save(p1);

            Part p2 = new Part();
            p2.setOrder(order1);
            p2.setName("Engine Oil 5W-40");
            p2.setQuantity(5);
            p2.setUnitPrice(12.00);
            partRepository.save(p2);

            // ── Completed Order 2 (v2 - 1 month ago) ──────────────────
            ServiceOrder order2 = new ServiceOrder();
            order2.setVehicle(v2);
            order2.setStatus("COMPLETED");
            order2.setCreatedAt(LocalDateTime.now().minusMonths(1).minusDays(5));
            order2.setCompletedAt(LocalDateTime.now().minusMonths(1));
            serviceOrderRepository.save(order2);

            WorkItem wi3 = new WorkItem();
            wi3.setOrder(order2);
            wi3.setDescription("Replace air filter");
            wi3.setEstimatedHours(0.5);
            wi3.setMechanicName("Bogdan");
            wi3.setStatus("DONE");
            workItemRepository.save(wi3);

            WorkItem wi4 = new WorkItem();
            wi4.setOrder(order2);
            wi4.setDescription("Wheel alignment");
            wi4.setEstimatedHours(1.0);
            wi4.setMechanicName("Bogdan");
            wi4.setStatus("DONE");
            workItemRepository.save(wi4);

            Part p3 = new Part();
            p3.setOrder(order2);
            p3.setName("Air Filter");
            p3.setQuantity(1);
            p3.setUnitPrice(25.00);
            partRepository.save(p3);

            Part p4 = new Part();
            p4.setOrder(order2);
            p4.setName("Wheel Alignment Kit");
            p4.setQuantity(1);
            p4.setUnitPrice(80.00);
            partRepository.save(p4);

            // ── Completed Order 3 (v3 - this month) ───────────────────
            ServiceOrder order3 = new ServiceOrder();
            order3.setVehicle(v3);
            order3.setStatus("COMPLETED");
            order3.setCreatedAt(LocalDateTime.now().minusDays(10));
            order3.setCompletedAt(LocalDateTime.now().minusDays(8));
            serviceOrderRepository.save(order3);

            WorkItem wi5 = new WorkItem();
            wi5.setOrder(order3);
            wi5.setDescription("Replace spark plugs");
            wi5.setEstimatedHours(2.0);
            wi5.setMechanicName("Stefan");
            wi5.setStatus("DONE");
            workItemRepository.save(wi5);

            Part p5 = new Part();
            p5.setOrder(order3);
            p5.setName("Spark Plugs");
            p5.setQuantity(4);
            p5.setUnitPrice(15.00);
            partRepository.save(p5);

            // ── Open Order (v1 - for live 409 demo) ───────────────────
            ServiceOrder order4 = new ServiceOrder();
            order4.setVehicle(v1);
            order4.setStatus("OPEN");
            order4.setCreatedAt(LocalDateTime.now().minusDays(1));
            serviceOrderRepository.save(order4);

            WorkItem wi6 = new WorkItem();
            wi6.setOrder(order4);
            wi6.setDescription("Replace timing belt");
            wi6.setEstimatedHours(3.0);
            wi6.setMechanicName("Bogdan");
            wi6.setStatus("IN_PROGRESS");
            workItemRepository.save(wi6);

        }
    }

