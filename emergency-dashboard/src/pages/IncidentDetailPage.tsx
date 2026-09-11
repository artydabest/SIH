import { useParams } from "react-router-dom";
import { useEmergencyData } from "../context/EmergencyDataContext";
import IncidentDetail from "../components/IncidentDetail";
import "../styles/page.css";

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { emergencies, updatingId, updateStatus } = useEmergencyData();

  const emergency = emergencies.find((e) => e._id === id);

  return (
    <div className="page">
      <IncidentDetail
        emergency={emergency}
        updatingId={updatingId}
        onStatusUpdate={(incidentId, nextStatus) =>
          void updateStatus(incidentId, nextStatus)
        }
        backTo="/incidents"
      />
    </div>
  );
}
