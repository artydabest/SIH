import { useEffect, useMemo } from "react";
import L from "leaflet";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import type { Emergency, EmergencyStatus } from "../types/emergency";
import { formatCoords, formatRelative } from "../lib/format";
import "../styles/rescue-map.css";

const EMERGENCY_COLOR: Record<EmergencyStatus, string> = {
  NEW: "#ff3b30",
  ACKNOWLEDGED: "#ff9f0a",
  RESPONDING: "#0a84ff",
  RESOLVED: "#30d158",
};

/**
 * Marker types from the brief. Emergency positions are real backend data.
 * Relay devices and the responder unit are simulated around each emergency
 * (the backend does not provide them) and are labeled as such in popups.
 */
function relayOffsets(emergency: Emergency): L.LatLngExpression[] {
  const jitter = (emergency._id.charCodeAt(0) % 5) + 3; // stable per device
  const d = jitter * 0.0016;
  return [
    [emergency.latitude + d, emergency.longitude - d * 0.7],
    [emergency.latitude - d * 0.6, emergency.longitude + d],
    [emergency.latitude + d * 0.3, emergency.longitude + d * 0.9],
  ];
}

const emergencyIcon = (status: EmergencyStatus) =>
  L.divIcon({
    className: "map-marker",
    html: `<span class="map-marker__emergency ${status === "NEW" ? "map-marker__emergency--pulse" : ""}" style="--marker-color:${EMERGENCY_COLOR[status]}"></span>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [0, -12],
  });

const relayIcon = L.divIcon({
  className: "map-marker",
  html: `<span class="map-marker__relay"></span>`,
  iconSize: [12, 12],
  iconAnchor: [6, 6],
  popupAnchor: [0, -8],
});

const responderIcon = L.divIcon({
  className: "map-marker",
  html: `<span class="map-marker__responder"></span>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
  popupAnchor: [0, -8],
});

const DEFAULT_CENTER: L.LatLngExpression = [12.9716, 77.5946];

interface MapFocusProps {
  emergencies: Emergency[];
  selected: Emergency | null;
}

/** Centers the map: fly to selection, otherwise fit all emergencies. */
function MapFocus({ emergencies, selected }: MapFocusProps) {
  const map = useMap();
  const signature = emergencies.map((e) => e._id).join(",");

  useEffect(() => {
    if (selected) {
      map.flyTo([selected.latitude, selected.longitude], 15, { duration: 0.8 });
    } else if (emergencies.length > 0) {
      const bounds = L.latLngBounds(
        emergencies.map((e) => [e.latitude, e.longitude] as L.LatLngTuple)
      );
      map.fitBounds(bounds, { padding: [45, 45], maxZoom: 15 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [signature, selected?._id, map]);

  return null;
}

interface RescueMapProps {
  emergencies: Emergency[];
  selectedId: string | null;
  onSelect: (emergency: Emergency) => void;
}

export default function RescueMap({
  emergencies,
  selectedId,
  onSelect,
}: RescueMapProps) {
  const selected = useMemo(
    () => emergencies.find((e) => e._id === selectedId) ?? null,
    [emergencies, selectedId]
  );

  const initialCenter: L.LatLngExpression =
    emergencies[0] != null
      ? [emergencies[0].latitude, emergencies[0].longitude]
      : DEFAULT_CENTER;

  return (
    <div className="rescue-map">
      <MapContainer
        center={initialCenter}
        zoom={emergencies.length > 0 ? 12 : 13}
        scrollWheelZoom
        className="rescue-map__canvas"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
        {emergencies.map((emergency) => {
          const isActive = emergency.status !== "RESOLVED";
          return (
            <div key={emergency._id}>
              <Marker
                position={[emergency.latitude, emergency.longitude]}
                icon={emergencyIcon(emergency.status)}
                eventHandlers={{ click: () => onSelect(emergency) }}
              >
                <Popup>
                  <div className="map-popup">
                    <strong>Device #{emergency.deviceId}</strong>
                    <span>Status: {emergency.status}</span>
                    <span>Detected {formatRelative(emergency.createdAt)}</span>
                    <span className="mono">{formatCoords(emergency.latitude, emergency.longitude)}</span>
                  </div>
                </Popup>
              </Marker>
              {isActive &&
                relayOffsets(emergency).map((position, index) => (
                  <Marker
                    key={`${emergency._id}-relay-${index}`}
                    position={position}
                    icon={relayIcon}
                  >
                    <Popup>
                      <div className="map-popup">
                        <strong>Relay device {index + 1}</strong>
                        <span className="map-popup__demo">SIMULATED — nearby iPhone relay</span>
                        <span>Detected emergency beacon</span>
                      </div>
                    </Popup>
                  </Marker>
                ))}
            </div>
          );
        })}
        <Marker position={[12.9698, 77.6178]} icon={responderIcon}>
          <Popup>
            <div className="map-popup">
              <strong>Responder Unit 1</strong>
              <span className="map-popup__demo">SIMULATED position</span>
            </div>
          </Popup>
        </Marker>
        <MapFocus emergencies={emergencies} selected={selected} />
      </MapContainer>

      <div className="map-legend" aria-hidden="true">
        <span><i className="legend-dot legend-dot--emergency" /> Emergency</span>
        <span><i className="legend-dot legend-dot--relay" /> Relay device</span>
        <span><i className="legend-dot legend-dot--responder" /> Responder</span>
      </div>
    </div>
  );
}
