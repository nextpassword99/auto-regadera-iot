from sqlalchemy.orm import Session
from . import models, schemas
from datetime import datetime, timedelta

# --- SensorReading CRUD ---

def get_latest_sensor_reading(db: Session):
    return db.query(models.SensorReading).order_by(models.SensorReading.timestamp.desc()).first()

def get_sensor_readings(db: Session, skip: int = 0, limit: int = 100):
    return db.query(models.SensorReading).order_by(models.SensorReading.timestamp.desc()).offset(skip).limit(limit).all()

def create_sensor_reading(db: Session, reading: schemas.SensorReadingCreate):
    db_reading = models.SensorReading(**reading.dict())
    db.add(db_reading)
    db.commit()
    db.refresh(db_reading)
    return db_reading

def get_readings_by_time_range(db: Session, start_date: datetime, end_date: datetime):
    return db.query(models.SensorReading).filter(
        models.SensorReading.timestamp >= start_date,
        models.SensorReading.timestamp <= end_date
    ).order_by(models.SensorReading.timestamp.asc()).all()


# --- WateringEvent CRUD ---

def create_watering_event(db: Session, event: schemas.WateringEventCreate):
    db_event = models.WateringEvent(**event.dict())
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return db_event

def get_watering_events(db: Session, skip: int = 0, limit: int = 25):
    return db.query(models.WateringEvent).order_by(models.WateringEvent.start_time.desc()).offset(skip).limit(limit).all()
