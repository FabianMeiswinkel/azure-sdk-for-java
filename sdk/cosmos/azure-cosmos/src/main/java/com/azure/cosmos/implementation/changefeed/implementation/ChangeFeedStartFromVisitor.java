package com.azure.cosmos.implementation.changefeed.implementation;

abstract class ChangeFeedStartFromVisitor {
    public abstract void Visit(ChangeFeedStartFromNowImpl startFromNow);
    public abstract void Visit(ChangeFeedStartFromPointInTimeImpl startFromTime);
    public abstract void Visit(ChangeFeedStartFromContinuationImpl startFromContinuation);
    public abstract void Visit(ChangeFeedStartFromBeginningImpl startFromBeginning);
}