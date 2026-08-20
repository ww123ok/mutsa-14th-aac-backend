package mutsa.hackathon.service;

import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardImageSource;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyRewardGenerationServiceTest {

    @Mock
    private WeeklyRewardClaimService claimService;
    @Mock
    private WeeklyRewardRepository weeklyRewardRepository;
    @Mock
    private OpenAiWeeklyVisualPlanGenerator visualPlanGenerator;
    @Mock
    private FallbackWeeklyVisualPlanFactory fallbackVisualPlanFactory;
    @Mock
    private OpenAiWeeklyImageGenerator imageGenerator;
    @Mock
    private OpenAiWeeklyRewardResultTextGenerator resultTextGenerator;
    @Mock
    private FallbackWeeklyRewardResultTextFactory fallbackResultTextFactory;
    @Mock
    private WeeklyImageStorage imageStorage;
    @Mock
    private WeeklyRewardCompletionService completionService;

    private WeeklyRewardGenerationService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyRewardGenerationService(
                claimService,
                weeklyRewardRepository,
                visualPlanGenerator,
                fallbackVisualPlanFactory,
                imageGenerator,
                resultTextGenerator,
                fallbackResultTextFactory,
                imageStorage,
                completionService
        );
    }

    @Test
    void 시각기획과_이미지를_먼저_만든뒤_최종문구를_생성하고_저장한다() {
        WeeklyRewardGenerationContext context = context();
        WeeklyVisualPlan visualPlan = visualPlan();
        GeneratedWeeklyImage image = image();
        WeeklyRewardResultText resultText = resultText();
        StoredWeeklyImage storedImage = new StoredWeeklyImage(
                "weekly/10/result.webp",
                "image/webp"
        );

        when(claimService.claim(10L)).thenReturn(
                WeeklyRewardClaimService.ClaimResult.claimed(context)
        );
        when(weeklyRewardRepository.findByUserIdAndWeekStartDate(
                20L,
                LocalDate.of(2026, 7, 27)
        )).thenReturn(Optional.empty());
        when(visualPlanGenerator.generate(context, null)).thenReturn(visualPlan);
        when(imageGenerator.generate(context, visualPlan)).thenReturn(image);
        when(resultTextGenerator.generate(context, visualPlan, image))
                .thenReturn(resultText);
        when(imageStorage.store(context, image)).thenReturn(storedImage);

        service.generate(10L);

        InOrder order = inOrder(
                claimService,
                visualPlanGenerator,
                imageGenerator,
                resultTextGenerator,
                imageStorage,
                completionService
        );

        order.verify(claimService).claim(10L);
        order.verify(visualPlanGenerator).generate(context, null);
        order.verify(imageGenerator).generate(context, visualPlan);
        order.verify(resultTextGenerator).generate(context, visualPlan, image);
        order.verify(imageStorage).store(context, image);
        order.verify(completionService).complete(
                10L,
                resultText,
                image,
                storedImage
        );

        verify(fallbackVisualPlanFactory, never()).create(context, null);
        verify(fallbackResultTextFactory, never()).create(
                context,
                visualPlan
        );
    }


    @Test
    void 이미지_생성_실패시_generic_fallback없이_FAILED로_처리한다() {
        WeeklyRewardGenerationContext context = context();
        WeeklyVisualPlan visualPlan = visualPlan();

        when(claimService.claim(10L)).thenReturn(
                WeeklyRewardClaimService.ClaimResult.claimed(context)
        );
        when(weeklyRewardRepository.findByUserIdAndWeekStartDate(
                20L,
                LocalDate.of(2026, 7, 27)
        )).thenReturn(Optional.empty());
        when(visualPlanGenerator.generate(context, null)).thenReturn(visualPlan);
        when(imageGenerator.generate(context, visualPlan)).thenThrow(
                new IllegalStateException("quality gate exhausted")
        );

        service.generate(10L);

        verify(completionService).fail(
                10L,
                "WEEKLY_REWARD_IMAGE_GENERATION_FAILED"
        );
        verify(resultTextGenerator, never()).generate(
                any(WeeklyRewardGenerationContext.class),
                any(WeeklyVisualPlan.class),
                any(GeneratedWeeklyImage.class)
        );
        verify(fallbackResultTextFactory, never()).create(
                context,
                visualPlan
        );
    }

    @Test
    void 직전주_카테고리를_이번주_시각기획에서_제외한다() {
        WeeklyRewardGenerationContext context = context();
        WeeklyReward previousReward = mock(WeeklyReward.class);

        when(claimService.claim(10L)).thenReturn(
                WeeklyRewardClaimService.ClaimResult.claimed(context)
        );
        when(weeklyRewardRepository.findByUserIdAndWeekStartDate(
                20L,
                LocalDate.of(2026, 7, 27)
        )).thenReturn(Optional.of(previousReward));
        when(previousReward.getGenerationStatus()).thenReturn(
                WeeklyRewardStatus.COMPLETED
        );
        when(previousReward.getCategoryKeyword()).thenReturn("픽셀아트");
        when(visualPlanGenerator.generate(
                context,
                WeeklyVisualCategory.PIXEL_ART
        )).thenReturn(visualPlan());

        service.generate(10L);

        verify(visualPlanGenerator).generate(
                context,
                WeeklyVisualCategory.PIXEL_ART
        );
    }

    private WeeklyRewardGenerationContext context() {
        return new WeeklyRewardGenerationContext(
                10L,
                20L,
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
                List.of(
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 3),
                                "팀 작업을 정리했다.",
                                "#D6A45C",
                                List.of("작업")
                        ),
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 5),
                                "저녁에 동네를 걸었다.",
                                "#6A8FB3",
                                List.of("산책")
                        ),
                        new WeeklyRewardGenerationContext.DayRecord(
                                LocalDate.of(2026, 8, 7),
                                "집에서 쉬었다.",
                                "#C9878A",
                                List.of("휴식")
                        )
                )
        );
    }

    private WeeklyVisualPlan visualPlan() {
        return new WeeklyVisualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                String.join(
                        " ",
                        java.util.Collections.nCopies(80, "visual")
                )
        );
    }

    private GeneratedWeeklyImage image() {
        return new GeneratedWeeklyImage(
                new byte[]{1, 2, 3},
                "image/webp",
                "webp",
                WeeklyRewardImageSource.AI
        );
    }

    private WeeklyRewardResultText resultText() {
        return new WeeklyRewardResultText(
                "작업과 산책이 이어진 한 주",
                "이번 주에는 작업을 정리하는 시간이 있었습니다. "
                        + "저녁에는 동네를 걷거나 집에서 쉬었습니다.",
                "그래픽 포스터",
                List.of("작업 정리", "저녁 산책", "휴식")
        );
    }
}
