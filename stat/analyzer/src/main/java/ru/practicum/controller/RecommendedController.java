package ru.practicum.controller;


import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stats.analyzer.RecommendationsControllerGrpc;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;
import ru.practicum.model.EventToRecommend;
import ru.practicum.service.RecommendedService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendedController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendedService recommendedService;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<EventToRecommend> list = recommendedService.getSimilarEvents(request);
            for (EventToRecommend re : list) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(re.eventId())
                        .setScore(re.score())
                        .build();
                responseObserver.onNext(proto);
            }
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("Недопустимый аргумент в getSimilarEvents: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e))
            );
        } catch (Exception e) {
            log.error("Непредвиденная ошибка в getSimilarEvents: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.UNKNOWN.withDescription("Произошла непредвиденная ошибка").withCause(e))
            );
        }
    }

    @Override
    public void getRecommendations(UserPredictionsRequestProto request,
                                   StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<EventToRecommend> list = recommendedService.getRecommendationsForUser(request);
            for (EventToRecommend re : list) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(re.eventId())
                        .setScore(re.score())
                        .build();
                responseObserver.onNext(proto);
            }
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("Недопустимый аргумент в getRecommendationsForUser: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e))
            );
        } catch (Exception e) {
            log.error("Непредвиденная ошибка в  getRecommendationsForUser: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.UNKNOWN.withDescription("Произошла непредвиденная ошибка").withCause(e))
            );
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<EventToRecommend> list = recommendedService.getInteractionsCount(request);
            for (EventToRecommend re : list) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(re.eventId())
                        .setScore(re.score())
                        .build();
                responseObserver.onNext(proto);
            }
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("Недопустимый аргумент в getInteractionsCount: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e))
            );
        } catch (Exception e) {
            log.error("Непредвиденная ошибка в getInteractionsCount: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.UNKNOWN.withDescription("Произошла непредвиденная ошибка").withCause(e))
            );
        }
    }
}