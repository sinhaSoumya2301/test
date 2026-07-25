import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMyShifts } from "@/hooks/useShifts";
import { useColleagues } from "@/hooks/useColleagues";
import { useCreateSwapRequest } from "@/hooks/useSwapRequests";
import { apiErrorMessage } from "@/api/client";

// Mutual shift-for-shift swaps (targetShiftId) are supported by the API but intentionally left
// out of this form — picking a colleague's specific shift would need a "view a colleague's
// shifts" endpoint that doesn't exist. This form only covers the "cover my shift" case
// (targetShiftId omitted), which the backend fully supports as-is.
const formSchema = z.object({
  requesterShiftId: z.string().min(1, "Pick a shift"),
  targetEmployeeId: z.string().min(1, "Pick a colleague"),
  reason: z.string().min(1, "A reason is required").max(1000),
});

type FormValues = z.infer<typeof formSchema>;

export function SwapRequestForm() {
  const { data: shifts, isLoading: shiftsLoading } = useMyShifts();
  const { data: colleagues, isLoading: colleaguesLoading } = useColleagues();
  const createSwap = useCreateSwapRequest();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({ resolver: zodResolver(formSchema) });

  const scheduledShifts = (shifts ?? []).filter((s) => s.status === "SCHEDULED");

  const onSubmit = async (values: FormValues) => {
    try {
      await createSwap.mutateAsync({ ...values, targetShiftId: null });
      reset();
    } catch (e) {
      setError("root", { message: apiErrorMessage(e, "Could not submit the swap request.") });
    }
  };

  if (shiftsLoading || colleaguesLoading) {
    return <p className="text-sm text-gray-500">Loading…</p>;
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3">
      <div>
        <label htmlFor="requesterShiftId" className="block text-sm font-medium text-gray-700">
          Your shift
        </label>
        <select
          id="requesterShiftId"
          {...register("requesterShiftId")}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
          defaultValue=""
        >
          <option value="" disabled>
            Select a shift
          </option>
          {scheduledShifts.map((shift) => (
            <option key={shift.id} value={shift.id}>
              {shift.shiftDate} · {shift.startTime.slice(0, 5)}–{shift.endTime.slice(0, 5)}
            </option>
          ))}
        </select>
        {errors.requesterShiftId && <p className="mt-1 text-sm text-red-600">{errors.requesterShiftId.message}</p>}
      </div>

      <div>
        <label htmlFor="targetEmployeeId" className="block text-sm font-medium text-gray-700">
          Swap with
        </label>
        <select
          id="targetEmployeeId"
          {...register("targetEmployeeId")}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
          defaultValue=""
        >
          <option value="" disabled>
            Select a colleague
          </option>
          {(colleagues ?? []).map((colleague) => (
            <option key={colleague.id} value={colleague.id}>
              {colleague.fullName}
            </option>
          ))}
        </select>
        {errors.targetEmployeeId && <p className="mt-1 text-sm text-red-600">{errors.targetEmployeeId.message}</p>}
      </div>

      <div>
        <label htmlFor="reason" className="block text-sm font-medium text-gray-700">
          Reason
        </label>
        <textarea
          id="reason"
          rows={2}
          {...register("reason")}
          className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
        />
        {errors.reason && <p className="mt-1 text-sm text-red-600">{errors.reason.message}</p>}
      </div>

      {errors.root && <p className="text-sm text-red-600">{errors.root.message}</p>}

      <button
        type="submit"
        disabled={isSubmitting || scheduledShifts.length === 0 || (colleagues ?? []).length === 0}
        className="rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {isSubmitting ? "Submitting…" : "Request swap"}
      </button>
    </form>
  );
}
