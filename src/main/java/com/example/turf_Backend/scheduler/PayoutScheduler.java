package com.example.turf_Backend.scheduler;

import com.example.turf_Backend.enums.BatchStatus;
import com.example.turf_Backend.repository.PayoutBatchRepository;
import com.example.turf_Backend.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutScheduler {
    private final PayoutService payoutService;
    private final PayoutBatchRepository payoutBatchRepository;

    //Runs every wednesday at 2:00 AM
    @Scheduled(cron = "0 0 2 * * WED")
    public void createWeeklyPayoutBatch()
    {
        LocalDate weekStart=LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd=LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);

        log.info("Scheduled payout batch creation triggered for week {} -> {}",weekStart,weekEnd);

        try
        {
            var response=payoutService.createWeeklyBatch(weekStart,weekEnd);
            log.info("Weekly payout batch created. batchId={}, totalAmount={}",response.getBatchId(),response.getTotalAmount());
        }catch (Exception e)
        {
            log.error("Failed to create weekly payout batch for {}-> {} :: {}",weekStart,weekEnd,e.getMessage(),e);
        }
    }
    @Scheduled(cron = "0 */5 * * * *")
    public void processApprovedBatches()
    {
        List<Long> batchIds=payoutBatchRepository.findIdsByStatus(BatchStatus.APPROVED);
        for (Long batchId : batchIds)
        {
            payoutService.startProcessing(batchId);
        }
    }
}
