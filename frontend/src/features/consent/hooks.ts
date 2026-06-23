import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listConsents, recordConsent, type Consent, type ConsentType } from "./api";

const CONSENT_VERSION = "v1.0";

export const consentKeys = {
  list: (patientId: string) => ["consents", patientId] as const,
};

export function useConsents(patientId: string | undefined) {
  return useQuery({
    queryKey: consentKeys.list(patientId ?? ""),
    queryFn: () => listConsents(patientId!),
    enabled: !!patientId,
  });
}

export function useSetConsent(patientId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ consentType, grant }: { consentType: ConsentType; grant: boolean }) =>
      recordConsent(
        patientId,
        grant
          ? { consentType, version: CONSENT_VERSION, grantedAt: new Date().toISOString() }
          : { consentType, version: CONSENT_VERSION, revokedAt: new Date().toISOString() }
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: consentKeys.list(patientId) }),
  });
}

// Current state of a consent type = its latest event is a grant that has not been revoked.
export function isConsentActive(consents: Consent[] | undefined, type: ConsentType): boolean {
  const latest = consents?.find((c) => c.consentType === type); // list is newest-first
  return !!latest && !!latest.grantedAt && !latest.revokedAt;
}
