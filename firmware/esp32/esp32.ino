#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>

const char *ssid = "weeeeee";
const char *password = "123";

WebServer server(80);

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

int umbralLuz = 2000;
int duracionRiego = 10000;
unsigned long intervaloRiego = 60000;

bool bombaEncendida = false;
bool modoManual = false;
unsigned long tiempoUltimoRiego = 0;

void configurarServidor();
void manejarStatus();
void manejarStart();
void manejarStop();
void manejarGetConfig();
void manejarPostConfig();
int obtenerUmbralHumedad();
void manejarRiegoAutomatico();
String tipoSueloToString(TipoSuelo tipo);
TipoSuelo stringToTipoSuelo(const String &tipo);

void setup()
{
    Serial.begin(115200);

    pinMode(PIN_PUMP_IN1, OUTPUT);
    pinMode(PIN_PUMP_IN2, OUTPUT);
    pinMode(PIN_PUMP_EN, OUTPUT);
    apagarBomba();

    WiFi.begin(ssid, password);
    Serial.print("Conectando a WiFi...");
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\n✅ Conectado a WiFi");
    Serial.println("IP: " + WiFi.localIP().toString());

    configurarServidor();
}

void loop()
{
    server.handleClient();

    if (!modoManual && millis() - tiempoUltimoRiego >= intervaloRiego)
    {
        tiempoUltimoRiego = millis();
        manejarRiegoAutomatico();
    }
}

void configurarServidor()
{
    server.on("/status", HTTP_GET, manejarStatus);
    server.on("/start", HTTP_POST, manejarStart);
    server.on("/stop", HTTP_POST, manejarStop);
    server.on("/config", HTTP_GET, manejarGetConfig);
    server.on("/config", HTTP_POST, manejarPostConfig);
    server.begin();
    Serial.println("🌐 Servidor HTTP iniciado");
}

void manejarRiegoAutomatico()
{
    int humedad = analogRead(PIN_SENSOR_HUMEDAD);
    int luz = analogRead(PIN_SENSOR_LUZ);

    Serial.printf("🔍 Humedad: %d | Luz: %d\n", humedad, luz);

    if (humedad > obtenerUmbralHumedad() && luz >= umbralLuz)
    {
        Serial.println("🚿 Activando riego automático");
        encenderBomba();
        delay(duracionRiego);
        apagarBomba();
    }
    else
    {
        Serial.println("🌿 No se requiere riego");
    }
}

void encenderBomba()
{
    digitalWrite(PIN_PUMP_IN1, HIGH);
    digitalWrite(PIN_PUMP_IN2, LOW);
    digitalWrite(PIN_PUMP_EN, HIGH);
    bombaEncendida = true;
}

void apagarBomba()
{
    digitalWrite(PIN_PUMP_EN, LOW);
    digitalWrite(PIN_PUMP_IN1, LOW);
    digitalWrite(PIN_PUMP_IN2, LOW);
    bombaEncendida = false;
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

void manejarStatus()
{
    int humedad = analogRead(PIN_SENSOR_HUMEDAD);
    int luz = analogRead(PIN_SENSOR_LUZ);

    DynamicJsonDocument json(256);
    json["humedad"] = humedad;
    json["luz"] = luz;
    json["nivel_luz"] = luz >= umbralLuz ? "claro" : "oscuro";
    json["bomba"] = bombaEncendida;
    json["modo"] = modoManual ? "manual" : "automatico";
    json["suelo"] = tipoSueloToString(tipoSuelo);

    String response;
    serializeJson(json, response);
    server.send(200, "application/json", response);
}

void manejarStart()
{
    modoManual = true;
    encenderBomba();
    server.send(200, "application/json", "{\"mensaje\":\"Riego manual activado\"}");
}

void manejarStop()
{
    modoManual = false;
    apagarBomba();
    server.send(200, "application/json", "{\"mensaje\":\"Riego manual detenido\"}");
}

void manejarGetConfig()
{
    DynamicJsonDocument json(256);
    json["tipoSuelo"] = tipoSueloToString(tipoSuelo);
    json["umbralLuz"] = umbralLuz;
    json["duracionRiego"] = duracionRiego;
    json["intervaloRiego"] = intervaloRiego;

    String response;
    serializeJson(json, response);
    server.send(200, "application/json", response);
}

void manejarPostConfig()
{
    if (server.args() == 0)
    {
        server.send(400, "application/json", "{\"error\":\"Falta cuerpo JSON\"}");
        return;
    }

    DynamicJsonDocument json(512);
    DeserializationError err = deserializeJson(json, server.arg(0));
    if (err)
    {
        server.send(400, "application/json", "{\"error\":\"JSON inválido\"}");
        return;
    }

    if (json.containsKey("tipoSuelo"))
    {
        tipoSuelo = stringToTipoSuelo(json["tipoSuelo"]);
    }
    if (json.containsKey("umbralLuz"))
    {
        umbralLuz = json["umbralLuz"];
    }
    if (json.containsKey("duracionRiego"))
    {
        duracionRiego = json["duracionRiego"];
    }
    if (json.containsKey("intervaloRiego"))
    {
        intervaloRiego = json["intervaloRiego"];
    }

    server.send(200, "application/json", "{\"mensaje\":\"Configuración actualizada\"}");
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

TipoSuelo stringToTipoSuelo(const String &tipo)
{
    if (tipo == "arenoso")
        return ARENOSO;
    if (tipo == "franco")
        return FRANCO;
    if (tipo == "arcilloso")
        return ARCILLOSO;
    return FRANCO;
}
