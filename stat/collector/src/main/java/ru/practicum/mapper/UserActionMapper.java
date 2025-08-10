package ru.practicum.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;


public class UserActionMapper {

    public static UserActionAvro toAvro(UserActionProto proto) {
        Instant timestampMillis = Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos());

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(toAvroActionType(proto.getActionType()))
                .setTimestamp(timestampMillis)
                .build();
    }

    private static ActionTypeAvro toAvroActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> ActionTypeAvro.VIEW;
        };
    }
}