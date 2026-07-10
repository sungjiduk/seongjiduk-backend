package com.sungjiduk.backend.admin.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sungjiduk.backend.admin.dto.request.AdminMajorContentSpotRequest;
import com.sungjiduk.backend.admin.dto.request.AdminMajorUserRateRequest;
import com.sungjiduk.backend.admin.dto.request.Duration;
import com.sungjiduk.backend.admin.dto.response.AdminMajorContentSpotResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorRateResponse;
import com.sungjiduk.backend.admin.dto.response.AdminMajorUserResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.event.repository.UsageEventRepository;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.repository.PilgrimageSpotRepository;
import com.sungjiduk.backend.trip.repository.TripPlanRepository;
import com.sungjiduk.backend.trip.repository.TripStopRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminMajorService {
    private final UsageEventRepository usageEventRepository;
    private final TripPlanRepository tripPlanRepository;
    private final TripStopRepository tripStopRepository;
    private final PilgrimageSpotRepository pilgrimageSpotRepository;

    public AdminMajorUserResponse user(AdminMajorUserRateRequest request) {
        LocalDate date = request.date();

        List<Long> values = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        if (request.duration() == Duration.week) {
            for (int i = 0; i < 7; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date);

                dates.add(date.toString());

                values.add(usageEventRepository.countUsageEventByOccurredAtBetween(start, end));

                date = date.minusDays(1);
            }
        }

        else if (request.duration() == Duration.month) {
            while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                date = date.minusDays(1);
            }

            for (int i = 0; i < 4; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date.plusDays(6));

                int weekNum = getWeekNum(date);

                dates.add(YearMonth.of(date.getYear(), date.getMonth()) + "-" + weekNum);

                values.add(usageEventRepository.countUsageEventByOccurredAtBetween(start, end));

                date = date.minusDays(7);
            }
        }

        else {
            for (int i = 0; i < 12; i += 1) {
                YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());

                LocalDateTime start = start(yearMonth.atDay(1));
                LocalDateTime end = end(yearMonth.atEndOfMonth());

                dates.add(yearMonth + "");
                values.add(usageEventRepository.countUsageEventByOccurredAtBetween(start, end));

                date = date.minusMonths(1);
            }
        }

        return new AdminMajorUserResponse(
            values.reversed(),
            dates.reversed()
        );
    }

    public AdminMajorRateResponse rate(AdminMajorUserRateRequest request) {
        LocalDate date = request.date();

        List<Double> values = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        if (request.duration() == Duration.week) {
            for (int i = 0; i < 7; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date);

                dates.add(date.toString());

                Long noneUser = tripPlanRepository.countByUserIsNullAndCreatedAtBetween(start, end);
                Long loginUser = tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(start, end);

                Double rating = (double) loginUser / (loginUser + noneUser);
                values.add(rating);

                date = date.minusDays(1);
            }
        }

        else if (request.duration() == Duration.month) {
            while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                date = date.minusDays(1);
            }

            for (int i = 0; i < 4; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date.plusDays(6));

                int weekNum = getWeekNum(date);

                dates.add(YearMonth.of(date.getYear(), date.getMonth()) + "-" + weekNum);

                Long noneUser = tripPlanRepository.countByUserIsNullAndCreatedAtBetween(start, end);
                Long loginUser = tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(start, end);

                Double rating = (double) loginUser / (loginUser + noneUser);
                values.add(rating);

                date = date.minusDays(7);
            }
        }

        else {
            for (int i = 0; i < 12; i += 1) {
                YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());

                LocalDateTime start = start(yearMonth.atDay(1));
                LocalDateTime end = end(yearMonth.atEndOfMonth());

                dates.add(yearMonth + "");

                Long noneUser = tripPlanRepository.countByUserIsNullAndCreatedAtBetween(start, end);
                Long loginUser = tripPlanRepository.countByUserIsNotNullAndCreatedAtBetween(start, end);

                Double rating = (double) loginUser / (loginUser + noneUser);
                values.add(rating);

                date = date.minusMonths(1);
            }
        }

        return new AdminMajorRateResponse(
            values.reversed(),
            dates.reversed()
        );
    }

    public AdminMajorContentSpotResponse content(AdminMajorContentSpotRequest request) {
        LocalDate date = request.date();

        List<List<AdminMajorContentSpotResponse.info>> values = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        if (request.duration() == Duration.week) {
            for (int i = 0; i < 7; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date);

                dates.add(date.toString());

                values.add(getContent(request.count(), start, end));

                date = date.minusDays(1);
            }
        }

        else if (request.duration() == Duration.month) {
            while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                date = date.minusDays(1);
            }

            for (int i = 0; i < 4; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date.plusDays(6));

                int weekNum = getWeekNum(date);

                dates.add(YearMonth.of(date.getYear(), date.getMonth()) + "-" + weekNum);

                values.add(getContent(request.count(), start, end));;

                date = date.minusDays(7);
            }
        }

        else {
            for (int i = 0; i < 12; i += 1) {
                YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());

                LocalDateTime start = start(yearMonth.atDay(1));
                LocalDateTime end = end(yearMonth.atEndOfMonth());

                values.add(getContent(request.count(), start, end));

                dates.add(yearMonth + "");

                date = date.minusMonths(1);
            }
        }

        return new AdminMajorContentSpotResponse(
            values.reversed(),
            dates.reversed()
        );
    }

    public AdminMajorContentSpotResponse spot(AdminMajorContentSpotRequest request) {
        LocalDate date = request.date();

        List<List<AdminMajorContentSpotResponse.info>> values = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        if (request.duration() == Duration.week) {
            for (int i = 0; i < 7; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date);

                dates.add(date.toString());

                values.add(getSpot(request.count(), start, end));

                date = date.minusDays(1);
            }
        }

        else if (request.duration() == Duration.month) {
            while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                date = date.minusDays(1);
            }

            for (int i = 0; i < 4; i += 1) {
                LocalDateTime start = start(date);
                LocalDateTime end = end(date.plusDays(6));

                int weekNum = getWeekNum(date);

                dates.add(YearMonth.of(date.getYear(), date.getMonth()) + "-" + weekNum);

                values.add(getSpot(request.count(), start, end));;

                date = date.minusDays(7);
            }
        }

        else {
            for (int i = 0; i < 12; i += 1) {
                YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonth());

                LocalDateTime start = start(yearMonth.atDay(1));
                LocalDateTime end = end(yearMonth.atEndOfMonth());

                values.add(getSpot(request.count(), start, end));

                dates.add(yearMonth + "");

                date = date.minusMonths(1);
            }
        }

        return new AdminMajorContentSpotResponse(
            values.reversed(),
            dates.reversed()
        );
    }

    private List<AdminMajorContentSpotResponse.info> getContent(long count, LocalDateTime startDate, LocalDateTime endDate) {
        List<AdminMajorContentSpotResponse.info> result = new ArrayList<>();

        List<Content> contentList = tripPlanRepository.findMostFrequentContent(startDate, endDate, PageRequest.of(0,
            (int)count));

        long allContentCount = tripPlanRepository.countAllByCreatedAtBetween(startDate, endDate);

        if (allContentCount == 0) {
            result.add(new AdminMajorContentSpotResponse.info("집계된 작품이 없습니다.", 0L, 0.0));

            return result;
        }

        long left = allContentCount;

        for (Content content : contentList) {
            long countContent = tripPlanRepository.countByContentAndCreatedAtBetween(content, startDate, endDate);
            left -= countContent;
            result.add(new AdminMajorContentSpotResponse.info(content.getTitle(), countContent,
                (double)(countContent / allContentCount)));
        }

        if (left > 0) {
            result.add(new AdminMajorContentSpotResponse.info("기타", left , (double)(left / allContentCount)));
        }

        return result;
    }

    private List<AdminMajorContentSpotResponse.info> getSpot(long count, LocalDateTime startDate, LocalDateTime endDate) {
        List<AdminMajorContentSpotResponse.info> result = new ArrayList<>();

        List<Long> spotList = tripStopRepository.findMostFrequentSpotToday(startDate, endDate, PageRequest.of(0,
            (int)count));

        long allCountSpot = tripStopRepository.countAllByCreatedAtBetween(startDate, endDate);

        if (allCountSpot == 0) {
            result.add(new AdminMajorContentSpotResponse.info("집계된 성지가 없습니다.", 0L, 0.0));

            return result;
        }

        long left = allCountSpot;

        for (Long spotId : spotList) {
            long countSpot = tripStopRepository.countByPilgrimageSpotIdAndCreatedAtBetween(spotId, startDate, endDate);
            PilgrimageSpot spot = pilgrimageSpotRepository.findById(spotId).orElse(null);

            if (spot == null) {
                throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
            }

            left -= countSpot;

            result.add(new AdminMajorContentSpotResponse.info(spot.getName(), countSpot,
                (double)(countSpot / allCountSpot)));
        }

        if (left > 0) {
            result.add(new AdminMajorContentSpotResponse.info("기타", left , (double)(left / allCountSpot)));
        }

        return result;
    }

    private int getWeekNum(LocalDate date) {
        LocalDate firstDayOfMonth = date.withDayOfMonth(1);

        // 2. 그 달의 '첫 번째 월요일'이 언제인지 찾습니다.
        LocalDate firstMonday = firstDayOfMonth;
        while (firstMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
            firstMonday = firstMonday.plusDays(1);
        }

        // 3. 비교하려는 날짜가 '첫 번째 월요일'보다 전이라면 ➡️ 무조건 "이전 달의 마지막 주"입니다.
        if (date.isBefore(firstMonday)) {
            LocalDate lastDayOfPrevMonth = firstDayOfMonth.minusDays(1);
            // 이전 달로 돌아가서 똑같은 기준으로 주차를 다시 계산합니다.
            return getWeekNum(lastDayOfPrevMonth);
        }

        // 4. '첫 번째 월요일' 이후라면 ➡️ (두 날짜 사이의 일주일 수) + 1이 현재 달의 주차가 됩니다.
        long weeksBetween = ChronoUnit.WEEKS.between(firstMonday, date);
        return Math.toIntExact(weeksBetween + 1);
    }

    private LocalDateTime start(LocalDate time) {
        return time.atStartOfDay(); // 0시 0분 0초
    }

    private LocalDateTime end(LocalDate time) {
        return time.atTime(LocalTime.MAX); // 23시 59분 59.99999...초
    }
}
