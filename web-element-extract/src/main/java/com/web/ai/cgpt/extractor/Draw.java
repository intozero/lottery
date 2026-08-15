package com.web.ai.cgpt.extractor;

import java.time.LocalDate;
import java.util.List;

public class Draw {

    private LocalDate drawDate;
    private List<Integer> whiteBalls;

    public Draw(LocalDate drawDate, List<Integer> nums, int pb, int pp) {
        this.drawDate = drawDate;
        this.powerBall = pb;
        this.powerPlay = pp;
        this.whiteBalls = nums;
    }

    public LocalDate getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(LocalDate drawDate) {
        this.drawDate = drawDate;
    }

    public List<Integer> getWhiteBalls() {
        return whiteBalls;
    }

    public void setWhiteBalls(List<Integer> whiteBalls) {
        this.whiteBalls = whiteBalls;
    }

    public int getPowerBall() {
        return powerBall;
    }

    public void setPowerBall(int powerBall) {
        this.powerBall = powerBall;
    }

    public int getPowerPlay() {
        return powerPlay;
    }

    public void setPowerPlay(int powerPlay) {
        this.powerPlay = powerPlay;
    }

    private int powerBall;
    private int powerPlay;

}