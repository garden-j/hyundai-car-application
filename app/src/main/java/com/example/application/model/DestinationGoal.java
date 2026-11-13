package com.example.application.model;

/** 목적지 기반 연습. origin이 null이면 현재 위치에서 출발한다. */
public class DestinationGoal {
    public Place origin;
    public Place destination;
    public boolean roundTrip;
}
