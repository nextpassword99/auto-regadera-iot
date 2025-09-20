from sqlalchemy.orm import Session
from typing import Optional
from datetime import datetime

import models, schemas

# --- SensorReading CRUD ---

def get_latest_sensor_reading(db: Session):
    print("🔍 CRUD: Buscando la última lectura en la BD.")
    return db.query(models.SensorReading).order_by(models.SensorReading.timestamp.desc()).first()

def get_sensor_readings(db: Session, skip: int = 0, limit: Optional[int] = 100, start_date: Optional[datetime] = None, end_date: Optional[datetime] = None):
    print(f"🔍 CRUD: Buscando lecturas con skip={skip}, limit={limit}, start={start_date}, end={end_date}")
    query = db.query(models.SensorReading)
    if start_date:
        query = query.filter(models.SensorReading.timestamp >= start_date)
    if end_date:
        query = query.filter(models.SensorReading.timestamp <= end_date)
    
    query = query.order_by(models.SensorReading.timestamp.desc())
    
    if skip:
        query = query.offset(skip)
    if limit:
        query = query.limit(limit)
        
    return query.all()

def create_sensor_reading(db: Session, reading: schemas.SensorReadingCreate):
    print(f"💾 CRUD: Creando nueva lectura en la BD.")
    db_reading = models.SensorReading(**reading.dict())
    db.add(db_reading)
    db.commit()
    db.refresh(db_reading)
    print(f"✔️ CRUD: Lectura creada con ID: {db_reading.id}")
    return db_reading

# --- WateringEvent CRUD ---

def create_watering_event(db: Session, event: schemas.WateringEventCreate):
    print(f"💾 CRUD: Creando nuevo evento de riego.")
    db_event = models.WateringEvent(**event.dict())
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    print(f"✔️ CRUD: Evento de riego creado con ID: {db_event.id}")
    return db_event

def get_watering_events(db: Session, skip: int = 0, limit: int = 25, start_date: Optional[datetime] = None, end_date: Optional[datetime] = None):
    print(f"🔍 CRUD: Buscando eventos de riego con skip={skip}, limit={limit}, start={start_date}, end={end_date}")
    query = db.query(models.WateringEvent)
    if start_date:
        query = query.filter(models.WateringEvent.start_time >= start_date)
    if end_date:
        query = query.filter(models.WateringEvent.start_time <= end_date)
    return query.order_by(models.WateringEvent.start_time.desc()).offset(skip).limit(limit).all()
