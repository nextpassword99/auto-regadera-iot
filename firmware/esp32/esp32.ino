#include <WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>

const char *ssid = "weeeeee";
const char *password = "123";

const char *host = "192.168.18.21";
const uint16_t port = 8000;
const char *path = "/ws/esp32-ingest";

const int PIN_SENSOR_HUMEDAD = 34;
const int PIN_SENSOR_LUZ = 35;
const int PIN_PUMP_IN1 = 26;
const int PIN_PUMP_IN2 = 27;
const int PIN_PUMP_EN = 25;

enum TipoSuelo
{
    ARENOSO,
    FRANCO,
    ARCILLOSO
};
TipoSuelo tipoSuelo = FRANCO;

bool bombaEncendida = false;
bool modoManual = false;

int umbralLuz = 2000;
int duracionRiego = 10000;
unsigned long intervaloRiego = 60000;
unsigned long tiempoUltimoRiego = 0;
unsigned long ultimoEnvio = 0;

WebSocketsClient webSocket;

void encenderBomba();
void apagarBomba();
int obtenerUmbralHumedad();
String tipoSueloToString(TipoSuelo tipo);
void enviarLectura();
void manejarWebSocketEvent(WStype_t type, uint8_t *payload, size_t length);

void setup()
{
    Serial.begin(115200);

    pinMode(PIN_PUMP_IN1, OUTPUT);
    pinMode(PIN_PUMP_IN2, OUTPUT);
    pinMode(PIN_PUMP_EN, OUTPUT);
    apagarBomba();

    Serial.println("🔌 Conectando a WiFi...");
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\n✅ WiFi conectado");
    Serial.print("IP local: ");
    Serial.println(WiFi.localIP());

    webSocket.begin(host, port, path);
    webSocket.onEvent(manejarWebSocketEvent);
    webSocket.setReconnectInterval(5000);

    Serial.println("🔁 Intentando conectar al WebSocket del servidor...");
}

void loop()
{
    webSocket.loop();

    if (!modoManual && millis() - tiempoUltimoRiego >= intervaloRiego)
    {
        tiempoUltimoRiego = millis();

        int humedad = analogRead(PIN_SENSOR_HUMEDAD);
        int luz = analogRead(PIN_SENSOR_LUZ);

        Serial.printf("📊 Lectura actual -> Humedad: %d | Luz: %d\n", humedad, luz);

        if (humedad > obtenerUmbralHumedad() && luz <= umbralLuz)
        {
            Serial.println("🚿 Riego automático activado");
            encenderBomba();
            delay(duracionRiego);
            apagarBomba();
        }
        else
        {
            Serial.println("🌿 Riego no necesario");
        }
    }

    if (millis() - ultimoEnvio >= 2000)
    {
        ultimoEnvio = millis();
        enviarLectura();
    }
}

void enviarLectura()
{
    int humedad = analogRead(PIN_SENSOR_HUMEDAD);
    int luz = analogRead(PIN_SENSOR_LUZ);

    DynamicJsonDocument doc(256);
    doc["humedad"] = humedad;
    doc["luz"] = luz;
    doc["bomba"] = bombaEncendida;
    doc["modo"] = modoManual ? "manual" : "automatico";
    doc["suelo"] = tipoSueloToString(tipoSuelo);

    String jsonStr;
    serializeJson(doc, jsonStr);
    webSocket.sendTXT(jsonStr);

    Serial.println("📤 Enviado al servidor:");
    Serial.println(jsonStr);
}

void manejarWebSocketEvent(WStype_t type, uint8_t *payload, size_t length)
{
    switch (type)
    {
    case WStype_CONNECTED:
        Serial.println("🟢 Conectado al WebSocket del servidor");
        break;

    case WStype_DISCONNECTED:
        Serial.println("🔴 Desconectado del WebSocket");
        break;

    case WStype_TEXT:
        Serial.print("📨 Mensaje recibido del servidor: ");
        Serial.println((char *)payload);

        if (strcmp((char *)payload, "start") == 0)
        {
            modoManual = true;
            encenderBomba();
        }
        else if (strcmp((char *)payload, "stop") == 0)
        {
            modoManual = false;
            apagarBomba();
        }
        break;

    default:
        break;
    }
}

void encenderBomba()
{
    digitalWrite(PIN_PUMP_IN1, HIGH);
    digitalWrite(PIN_PUMP_IN2, LOW);
    digitalWrite(PIN_PUMP_EN, HIGH);
    bombaEncendida = true;
    Serial.println("💧 Bomba encendida");
}

void apagarBomba()
{
    digitalWrite(PIN_PUMP_EN, LOW);
    digitalWrite(PIN_PUMP_IN1, LOW);
    digitalWrite(PIN_PUMP_IN2, LOW);
    bombaEncendida = false;
    Serial.println("🛑 Bomba apagada");
}

int obtenerUmbralHumedad()
{
    switch (tipoSuelo)
    {
    case ARENOSO:
        return 350;
    case FRANCO:
        return 500;
    case ARCILLOSO:
        return 650;
    default:
        return 500;
    }
}

String tipoSueloToString(TipoSuelo tipo)
{
    switch (tipo)
    {
    case ARENOSO:
        return "arenoso";
    case FRANCO:
        return "franco";
    case ARCILLOSO:
        return "arcilloso";
    default:
        return "desconocido";
    }
}
