import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  listMyClinics,
  listStaff,
  addStaff,
  removeStaff,
  verifyStaff,
  listCapabilities,
  setCapability,
  type AddStaffInput,
  type CapabilityType,
} from "./api";

export const clinicKeys = {
  myClinics: ["me", "clinics"] as const,
  staff: (clinicId: string) => ["clinic", clinicId, "staff"] as const,
  capabilities: (clinicId: string) => ["clinic", clinicId, "capabilities"] as const,
};

export function useMyClinics() {
  return useQuery({ queryKey: clinicKeys.myClinics, queryFn: listMyClinics });
}

export function useStaff(clinicId: string | undefined) {
  return useQuery({
    queryKey: clinicKeys.staff(clinicId ?? ""),
    queryFn: () => listStaff(clinicId!),
    enabled: !!clinicId,
  });
}

export function useCapabilities(clinicId: string | undefined) {
  return useQuery({
    queryKey: clinicKeys.capabilities(clinicId ?? ""),
    queryFn: () => listCapabilities(clinicId!),
    enabled: !!clinicId,
  });
}

export function useAddStaff(clinicId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: AddStaffInput) => addStaff(clinicId, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: clinicKeys.staff(clinicId) }),
  });
}

export function useRemoveStaff(clinicId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (staffId: string) => removeStaff(clinicId, staffId),
    onSuccess: () => qc.invalidateQueries({ queryKey: clinicKeys.staff(clinicId) }),
  });
}

export function useVerifyStaff(clinicId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (staffId: string) => verifyStaff(clinicId, staffId),
    onSuccess: () => qc.invalidateQueries({ queryKey: clinicKeys.staff(clinicId) }),
  });
}

export function useSetCapability(clinicId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ capability, enabled }: { capability: CapabilityType; enabled: boolean }) =>
      setCapability(clinicId, capability, enabled),
    onSuccess: () => qc.invalidateQueries({ queryKey: clinicKeys.capabilities(clinicId) }),
  });
}
