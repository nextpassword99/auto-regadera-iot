from sqlalchemy.orm import Session
from . import models, schemas
from datetime import datetime
from typing import Optional

# --- SensorReading CRUD ---

def get_latest_sensor_reading(db: Session):
    return db.query(models.SensorReading).order_by(models.SensorReading.timestamp.desc()).first()

def get_sensor_readings(db: Session, skip: int = 0, limit: int = 100, start_date: Optional[datetime] = None, end_date: Optional[datetime] = None):
    query = db.query(models.SensorReading)
    if start_date:
        query = query.filter(models.SensorReading.timestamp >= start_date)
    if end_date:
        query = query.filter(models.SensorReading.timestamp <= end_date)
    return query.order_by(models.SensorReading.timestamp.desc()).offset(skip).limit(limit).all()

def create_sensor_reading(db: Session, reading: schemas.SensorReadingCreate):
    db_reading = models.SensorReading(**reading.dict())
    db.add(db_reading)
    db.commit()
    db.refresh(db_reading)
    return db_reading

# --- WateringEvent CRUD ---

def create_watering_event(db: Session, event: schemas.WateringEventCreate):
    db_event = models.WateringEvent(**event.dict())
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return db_event

def get_watering_events(db: Session, skip: int = 0, limit: int = 25, start_date: Optional[datetime] = None, end_date: Optional[datetime] = None):
    query = db.query(models.WateringEvent)
    if start_date:
        query = query.filter(models.WateringEvent.start_time >= start_date)
    if end_date:
        query = query.filter(models.WateringEvent.start_time <= end_date)
    return query.order_by(models.WateringEvent.start_time.desc()).offset(skip).limit(limit).all()
