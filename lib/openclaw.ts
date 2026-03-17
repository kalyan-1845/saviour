import { ClawAgent, Skill } from 'openclaw';

/**
 * Emergency Coordination Skill
 * This skill allows the agent to notify hospitals and police stations
 * and coordinate green corridors.
 */
const emergencySkill: Skill = {
  name: 'emergency_coordination',
  description: 'Coordinate emergency response units and notify stakeholders',
  actions: [
    {
      name: 'notify_hospital',
      description: 'Send a high-priority alert to the destination hospital',
      parameters: {
        hospitalId: 'string',
        patientStatus: 'string',
        eta: 'number'
      },
      handler: async ({ hospitalId, patientStatus, eta }: { hospitalId: string; patientStatus: string; eta: number }) => {
        console.log(`[OpenClaw Agent] Notifying hospital ${hospitalId}: Patient ${patientStatus}, ETA ${eta}m`);
        // In real implementation, this would trigger a WhatsApp/SMS alert
        return { success: true, message: 'Hospital notified via emergency channel' };
      }
    },
    {
      name: 'alert_police_patrol',
      description: 'Alert nearest police patrol for green corridor assistance',
      parameters: {
        location: 'object',
        emergencyType: 'string'
      },
      handler: async ({ location, emergencyType }: { location: any; emergencyType: string }) => {
        console.log(`[OpenClaw Agent] Alerting police near ${JSON.stringify(location)} for ${emergencyType}`);
        return { success: true, message: 'Police patrol units alerted' };
      }
    }
  ]
};

let agent: ClawAgent | null = null;

export async function getEmergencyAgent() {
  if (!agent) {
    agent = new ClawAgent({
      name: 'SARATHI Emergency Coordinator',
      description: 'Autonomous agent for managing city-wide emergency responses',
      skills: [emergencySkill],
      model: 'llama-3.3-70b-versatile', // Using Groq via OpenClaw provider if configured
    });
  }
  return agent;
}

export async function coordinateEmergency(tripData: any) {
  const agent = await getEmergencyAgent();
  
  const prompt = `
    An emergency trip (ID: ${tripData.tripId}) has been initiated.
    Type: ${tripData.emergencyType}
    User: ${tripData.user.fullName} (${tripData.user.phone})
    Driver: ${tripData.driver.fullName} (${tripData.driver.phone})
    Destination: ${tripData.hospital?.name || tripData.policeStation?.name}
    ETA: ${tripData.etaMinutes} minutes
    
    Coordinate the response:
    1. Notify the destination unit.
    2. If it's a critical medical emergency (heart_attack, stroke), alert police for a green corridor.
  `;

  try {
    const result = await agent.process(prompt);
    console.log('[OpenClaw Agent] Coordination result:', result);
    return result;
  } catch (error) {
    console.error('[OpenClaw Agent] Coordination failed:', error);
    return null;
  }
}
