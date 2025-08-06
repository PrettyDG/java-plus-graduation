package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.practicum.kafka.MessageProducer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.UserActionMapper;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final MessageProducer messageProducer;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            UserActionAvro userActionAvro = UserActionMapper.toAvro(request);
            messageProducer.sendUserAction(userActionAvro);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException collectUserAction: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e))
            );
        } catch (Exception e) {
            log.error("error collectUserAction: {}", e.getMessage(), e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.UNKNOWN.withDescription("error").withCause(e))
            );
        }
    }
}