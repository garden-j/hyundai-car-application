package com.example.application.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.application.databinding.ContentScrollBinding;
import com.example.application.model.AverageDriveTime;
import com.example.application.model.DriverProfile;
import com.example.application.model.LicensePeriod;
import com.example.application.ui.common.BaseStepFragment;
import com.example.application.ui.common.Choices;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 0-2 운전은 얼마나 해 보셨어요? — 운전 경력(면허 취득 시기·연수 경험), 빈도(1~5), 1회 평균 시간.
 * 응답하지 않은 항목은 미입력으로 저장한다.
 */
public class ExperienceFragment extends BaseStepFragment {
    @Override
    protected View onCreateContent(LayoutInflater inflater, ViewGroup container) {
        setTitle("운전은 얼마나\n해 보셨어요?", "실제 운전 빈도와 경력을 알려 주세요.");
        setProgress(2, 4);
        showNav(true, false);
        DriverProfile p = services().profiles.get();
        LinearLayout list = ContentScrollBinding.inflate(inflater, frame.content, true).list;

        Set<LicensePeriod> license = EnumSet.noneOf(LicensePeriod.class);
        if (p.licensePeriod != null) {
            license.add(p.licensePeriod);
        }
        Set<String> lessons = new LinkedHashSet<>();
        if (p.hadDrivingLessons != null) {
            lessons.add(p.hadDrivingLessons ? "받아 봤어요" : "아직이에요");
        }
        Set<Integer> frequency = new LinkedHashSet<>();
        if (p.drivingFrequency != null) {
            frequency.add(p.drivingFrequency);
        }
        Set<AverageDriveTime> time = EnumSet.noneOf(AverageDriveTime.class);
        if (p.averageDriveTime != null) {
            time.add(p.averageDriveTime);
        }

        Choices.addChipSection(list, "운전 경력", Arrays.asList(LicensePeriod.values()), license, true, null);
        Choices.addChipSection(list, "도로 연수", Arrays.asList("받아 봤어요", "아직이에요"), s -> s, lessons, true, null);
        Choices.addChipSection(list, "운전 빈도 (1 가끔 · 5 자주)", Arrays.asList(1, 2, 3, 4, 5),
                String::valueOf, frequency, true, null);
        Choices.addChipSection(list, "한 번에 운전하는 시간", Arrays.asList(AverageDriveTime.values()), time, true, null);
        Choices.addChipSection(list, "익숙한 도로 (여러 개 선택)", Arrays.asList(com.example.application.model.RoadEnvironment.values()),
                p.familiarRoads, false, null);
        Choices.addChipSection(list, "주로 운전하는 목적 (여러 개 선택)", Arrays.asList(com.example.application.model.DrivingPurpose.values()),
                p.purposes, false, null);

        setPrimary("다음", () -> {
            p.licensePeriod = license.isEmpty() ? null : license.iterator().next();
            p.hadDrivingLessons = lessons.isEmpty() ? null : lessons.contains("받아 봤어요");
            p.drivingFrequency = frequency.isEmpty() ? null : frequency.iterator().next();
            p.averageDriveTime = time.isEmpty() ? null : time.iterator().next();
            services().profiles.save(p);
            nav().push(new DifficultiesFragment());
        });
        return null;
    }
}
