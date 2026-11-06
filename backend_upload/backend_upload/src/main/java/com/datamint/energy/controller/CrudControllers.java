package com.datamint.energy.controller;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import org.springframework.web.bind.annotation.*;

// Module 3 - /api/buildings, /api/departments, /api/meters | Module 5 - /api/tariffs, /api/bills | Module 9 - /api/equipment

@RestController @RequestMapping("/api/buildings")
class BuildingController extends CrudController<Building, BuildingRepository> { BuildingController(BuildingRepository r) { super(r); } }

@RestController @RequestMapping("/api/departments")
class DepartmentController extends CrudController<Department, DepartmentRepository> { DepartmentController(DepartmentRepository r) { super(r); } }

@RestController @RequestMapping("/api/meters")
class MeterController extends CrudController<Meter, MeterRepository> { MeterController(MeterRepository r) { super(r); } }

@RestController @RequestMapping("/api/equipment")
class EquipmentController extends CrudController<Equipment, EquipmentRepository> { EquipmentController(EquipmentRepository r) { super(r); } }

@RestController @RequestMapping("/api/tariffs")
class TariffController extends CrudController<Tariff, TariffRepository> { TariffController(TariffRepository r) { super(r); } }

@RestController @RequestMapping("/api/bills")
class BillController extends CrudController<EnergyBill, EnergyBillRepository> { BillController(EnergyBillRepository r) { super(r); } }
