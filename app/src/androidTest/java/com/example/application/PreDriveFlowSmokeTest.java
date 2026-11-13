package com.example.application;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.application.core.pleos.location.FakeLocationGateway;
import com.example.application.core.pleos.navi.FakeNaviGateway;
import com.example.application.core.pleos.vehicle.FakeVehicleGateway;
import com.example.application.core.vehiclecontrol.PresetController;
import com.example.application.di.ServiceLocator;
import com.example.application.domain.RouteCandidateGenerator;
import com.example.application.domain.SetupStep;
import com.example.application.model.Difficulty;
import com.example.application.model.DrivingSkill;
import com.example.application.model.Familiarity;
import com.example.application.model.PermissionType;
import com.example.application.model.PracticeGoalType;
import com.example.application.model.PracticeSetup;
import com.example.application.ui.setup.SetupViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 출발 전 흐름 후반부(체크리스트 → 프리셋 적용 → 출발) UI 스모크 테스트.
 * 가상 차량을 써서 기기 상태(안전벨트 등)와 무관하게 화면 전환과 적용 결과 처리를 검증한다.
 */
@RunWith(AndroidJUnit4.class)
public class PreDriveFlowSmokeTest {

    @Test
    public void checklistToPresetToDepart() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ServiceLocator sl = ServiceLocator.get();
                sl.vehicle = new FakeVehicleGateway();
                sl.navi = new FakeNaviGateway();
                sl.location = new FakeLocationGateway();
                sl.presets = new PresetController(sl.vehicle);
                for (PermissionType t : PermissionType.values()) {
                    sl.consents.record(t, true);
                }
                SetupViewModel vm = new ViewModelProvider(activity).get(SetupViewModel.class);
                vm.start(null);
                PracticeSetup s = vm.setup();
                s.goalTypes.add(PracticeGoalType.SKILL);
                s.skills.add(DrivingSkill.U_TURN);
                s.difficulty = Difficulty.EASY;
                s.familiarity = Familiarity.FAMILIAR;
                s.tension = 2;
                s.selectedRoute = RouteCandidateGenerator.generate(FakeLocationGateway.DEMO_LOCATION, s).get(0);
                activity.navigator().openSetupStep(SetupStep.CHECKLIST);
            });
            Thread.sleep(800);
            onView(withText("출발 전에\n확인해요")).check(matches(isDisplayed()));
            // 가상 차량은 첫 확인 후 안전벨트를 맨 상태가 된다
            onView(withText("차량 상태 다시 확인")).perform(click());
            Thread.sleep(500);
            onView(withText("다음")).perform(click());
            Thread.sleep(500);
            // 사용자 확인 항목이 남아 있으면 "계속" 대화상자가 뜬다
            onView(withText("계속")).perform(click());
            Thread.sleep(800);

            onView(withText("차 안을 이렇게\n맞춰 둘까요?")).check(matches(isDisplayed()));
            onView(withText("조작 연습 모드를 추천해요.")).check(matches(isDisplayed()));
            onView(withText("이대로 적용")).perform(click());
            Thread.sleep(2500);

            onView(withText("준비 끝!\n출발해요")).check(matches(isDisplayed()));
        }
    }
}
