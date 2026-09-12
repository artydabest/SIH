import { useEffect, useMemo, useState } from "react";
import L from "leaflet";
import {
  Circle,
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  Tooltip,
  useMap,
  useMapEvents,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import type { Emergency, EmergencyStatus } from "../types/emergency";
import type { Person } from "../types/person";
import type { SafeZone } from "../types/safezone";
import type { Volunteer } from "../types/volunteer";
import type { DisasterAlert } from "../types/alert";
import { DISASTER_LABEL } from "../types/alert";
import { SAFE_ZONE_KIND_LABEL } from "../types/safezone";
import { formatCoords, formatRelative } from "../lib/format";
import "../styles/rescue-map.css";

const EMERGENCY_COLOR: Record<EmergencyStatus, string> = {
  NEW: "#d93025",
  ACKNOWLEDGED: "#b06000",
  RESPONDING: "#0a84ff",
  RESOLVED: "#188038",
};

const emergencyIcon = (status: EmergencyStatus, selected: boolean) =>
  L.divIcon({
    className: "map-marker",
    html: `<span class="map-marker__emergency ${status === "NEW" ? "map-marker__emergency--pulse" : ""} ${selected ? "map-marker__emergency--selected" : ""}" style="--marker-color:${EMERGENCY_COLOR[status]}"></span>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [0, -12],
  });

const safeZoneIcon = L.divIcon({
  className: "map-marker",
  html: `<span class="map-marker__safezone"></span>`,
  iconSize: [14, 14],
  iconAnchor: [7, 7],
  popupAnchor: [0, -10],
});

const volunteerIcon = L.divIcon({
  className: "map-marker",
  html: `<span class="map-marker__volunteer"></span>`,
  iconSize: [13, 13],
  iconAnchor: [6.5, 6.5],
  popupAnchor: [0, -9],
});

const personIcon = (safe: boolean) =>
  L.divIcon({
    className: "map-marker",
    html: `<span class="map-marker__person ${safe ? "" : "map-marker__person--unsafe"}"></span>`,
    iconSize: [13, 13],
    iconAnchor: [6.5, 6.5],
    popupAnchor: [0, -9],
  });

const DEFAULT_CENTER: L.LatLngExpression = [12.9716, 77.5946];

/** Click-to-deselect: clicking empty map clears the current selection. */
function ClickToDeselect({ onDeselect }: { onDeselect: () => void }) {
  useMapEvents({
    click: () => onDeselect(),
  });
  return null;
}

interface MapFocusProps {
  emergencies: Emergency[];
  selected: Emergency | null;
}

/** Centers the map: fly to selection, otherwise fit all markers. */
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
  people: Person[];
  safeZones: SafeZone[];
  volunteers: Volunteer[];
  activeAlert?: DisasterAlert | null;
  selectedId: string | null;
  /** Called with the clicked emergency, or null when the map background is clicked. */
  onSelect: (emergency: Emergency | null) => void;
}

export default function RescueMap({
  emergencies,
  people,
  safeZones,
  volunteers,
  activeAlert = null,
  selectedId,
  onSelect,
}: RescueMapProps) {
  const [showEmergencies, setShowEmergencies] = useState(true);
  const [showSafeZones, setShowSafeZones] = useState(true);
  const [showVolunteers, setShowVolunteers] = useState(true);
  const [showPeople, setShowPeople] = useState(true);

  const selected = useMemo(
    () => emergencies.find((e) => e._id === selectedId) ?? null,
    [emergencies, selectedId]
  );

  const initialCenter: L.LatLngExpression =
    emergencies[0] != null
      ? [emergencies[0].latitude, emergencies[0].longitude]
      : DEFAULT_CENTER;

  const handleDeselect = () => onSelect(null);

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
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        />

        {showSafeZones &&
          safeZones.map((zone) => (
            <Marker
              key={zone._id}
              position={[zone.latitude, zone.longitude]}
              icon={safeZoneIcon}
            >
              <Tooltip direction="top" className="map-tooltip">
                {SAFE_ZONE_KIND_LABEL[zone.kind] ?? zone.kind}
              </Tooltip>
              <Popup>
                <div className="map-popup">
                  <strong>{zone.name}</strong>
                  <span>{SAFE_ZONE_KIND_LABEL[zone.kind] ?? zone.kind}</span>
                  {zone.capacity != null && zone.capacity > 0 && (
                    <span>Capacity: {zone.capacity}</span>
                  )}
                  <span className="mono">{formatCoords(zone.latitude, zone.longitude)}</span>
                </div>
              </Popup>
            </Marker>
          ))}

        {showVolunteers &&
          volunteers
            .filter((v) => v.available)
            .map((volunteer) => (
              <Marker
                key={volunteer._id}
                position={[volunteer.latitude, volunteer.longitude]}
                icon={volunteerIcon}
              >
                <Tooltip direction="top" className="map-tooltip">
                  Volunteer
                </Tooltip>
                <Popup>
                  <div className="map-popup">
                    <strong>{volunteer.name}</strong>
                    <span>Available</span>
                    {volunteer.phone && (
                      <span className="mono">☎ {volunteer.phone}</span>
                    )}
                  </div>
                </Popup>
              </Marker>
            ))}

        {showPeople &&
          people.map((person) => (
            <Marker
              key={person._id}
              position={[person.latitude, person.longitude]}
              icon={personIcon(person.safe)}
            >
              <Tooltip direction="top" className="map-tooltip">
                {person.name ?? person.deviceId} — {person.safe ? "safe" : "needs help"}
              </Tooltip>
              <Popup>
                <div className="map-popup">
                  <strong>{person.name ?? person.deviceId}</strong>
                  <span className={person.safe ? "map-popup__safe" : "map-popup__unsafe"}>
                    {person.safe ? "SAFE" : "NEEDS HELP"}
                  </span>
                  <span>Checked in {formatRelative(person.lastSeenAt)}</span>
                  <span className="mono">{formatCoords(person.latitude, person.longitude)}</span>
                </div>
              </Popup>
            </Marker>
          ))}

        {/* BIG RED DISASTER ZONE CIRCLE — from the active alert */}
        {activeAlert &&
          activeAlert.latitude != null &&
          activeAlert.longitude != null &&
          activeAlert.radiusKm != null && (
            <Circle
              center={[activeAlert.latitude, activeAlert.longitude]}
              radius={activeAlert.radiusKm * 1000}
              pathOptions={{
                color: "#d93025",
                weight: 3,
                fillColor: "#d93025",
                fillOpacity: 0.18,
                dashArray: "10 6",
              }}
            >
              <Popup>
                <div className="map-popup">
                  <strong>{DISASTER_LABEL[activeAlert.type] ?? activeAlert.type} ZONE</strong>
                  <span>{activeAlert.message}</span>
                  <span>Radius: {activeAlert.radiusKm} km</span>
                </div>
              </Popup>
            </Circle>
          )}

        {showEmergencies && (
          <>
            {emergencies
              .filter((e) => e.status === "NEW")
              .map((emergency) => (
                <Circle
                  key={`${emergency._id}-zone`}
                  center={[emergency.latitude, emergency.longitude]}
                  radius={350}
                  pathOptions={{
                    color: EMERGENCY_COLOR.NEW,
                    weight: 2,
                    fillColor: EMERGENCY_COLOR.NEW,
                    fillOpacity: 0.15,
                    dashArray: "6 5",
                  }}
                >
                  <Tooltip sticky className="map-tooltip">
                    Alert zone — device #{emergency.deviceId}
                  </Tooltip>
                </Circle>
              ))}

            {emergencies.map((emergency) => (
              <Marker
                key={emergency._id}
                position={[emergency.latitude, emergency.longitude]}
                icon={emergencyIcon(emergency.status, emergency._id === selectedId)}
                eventHandlers={{ click: () => onSelect(emergency) }}
              >
                <Popup>
                  <div className="map-popup">
                    <strong>Device #{emergency.deviceId}</strong>
                    <span>Status: {emergency.status}</span>
                    <span>
                      Confidence: {emergency.confidence != null && emergency.confidenceLevel
                        ? `${emergency.confidence}% ${emergency.confidenceLevel}`
                        : "—"}
                    </span>
                    <span>Detected {formatRelative(emergency.createdAt)}</span>
                    <span className="mono">
                      {formatCoords(emergency.latitude, emergency.longitude)}
                    </span>
                  </div>
                </Popup>
              </Marker>
            ))}
          </>
        )}

        <ClickToDeselect onDeselect={handleDeselect} />
        <MapFocus emergencies={emergencies} selected={selected} />
      </MapContainer>

      <div className="map-layers" role="group" aria-label="Map layer toggles">
        <label className="map-layer-toggle">
          <input
            type="checkbox"
            checked={showEmergencies}
            onChange={(e) => setShowEmergencies(e.target.checked)}
          />
          Alerts
        </label>
        <label className="map-layer-toggle">
          <input
            type="checkbox"
            checked={showSafeZones}
            onChange={(e) => setShowSafeZones(e.target.checked)}
          />
          Safe zones
        </label>
        <label className="map-layer-toggle">
          <input
            type="checkbox"
            checked={showVolunteers}
            onChange={(e) => setShowVolunteers(e.target.checked)}
          />
          Volunteers
        </label>
        <label className="map-layer-toggle">
          <input
            type="checkbox"
            checked={showPeople}
            onChange={(e) => setShowPeople(e.target.checked)}
          />
          Friends
        </label>
      </div>

      <div className="map-legend" aria-hidden="true">
        <span><i className="legend-dot legend-dot--emergency" /> Emergency</span>
        <span><i className="legend-dot legend-dot--safezone" /> Safe zone</span>
        <span><i className="legend-dot legend-dot--volunteer" /> Volunteer</span>
        <span><i className="legend-dot legend-dot--person" /> Friend</span>
      </div>
    </div>
  );
}
