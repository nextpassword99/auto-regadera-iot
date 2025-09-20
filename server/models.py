from sqlalchemy import Column, Integer, Float, DateTime, String, Boolean
from sqlalchemy.sql import func
from database import Base

class SensorReading(Base):
    __tablename__ = "sensor_readings"

    id = Column(Integer, primary_key=True, index=True)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())
    humidity = Column(Float, nullable=False)
    light = Column(Float, nullable=False)
    pump_status = Column(Boolean, nullable=False)
    mode = Column(String, nullable=False)
    soil_type = Column(String, nullable=False)

    def dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}

class WateringEvent(Base):
    __tablename__ = "watering_events"
    
    id = Column(Integer, primary_key=True, index=True)
    start_time = Column(DateTime(timezone=True), server_default=func.now())
    duration_seconds = Column(Integer, nullable=False)
    reason = Column(String) # 'automatico', 'manual'
