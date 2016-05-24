package de.mlessmann.network;

/**
 * Created by mark332 on 08.05.2016.
 * @author Life4YourGames
 */
public class Status {

    //100
    public static final int CONTINUE = 100;
    public static final String SCONTINUE = "continue";

    //101
    public static final int SWITCHPROTOCOLS = 101;
    public static final String SSWITCHINGPROTOCOLS = "switching_protocols";

    //102
    public static final int PROCESSING = 102;
    public static final String SPROCESSING = "processing";

    //103
    public static final int NOTIFY_DEV = 103;
    public static final String SNOTIFY_DEV = "notify_developer";

    //104
    public static final int NOTIFY_CLIENT = 104;
    public static final String SNOTIFY_CLIENT = "notify_client";

    //200
    public static final int OK = 200;
    public static final String SOK = "OK";

    //201
    public static final int CREATED = 201;
    public static final String SCREATED = "created";

    //202
    public static final int ACCEPTED = 202;
    public static final String SACCEPTED = "accepted";

    //203
    //
    //

    //204
    public static final int NOCONTENT = 204;
    public static final String SNOCONTENT = "no_content";

    //205
    public static final int RESETCONTENT = 205;
    public static final String SRESETCONTENT = "reset_content";

    //206
    //
    //

    //207
    //
    //

    //208
    //
    //

    //400
    public static final int BADREQUEST = 400;
    public static final String SBADREQUEST = "bad_request";

    //401
    public static final int UNAUTHORIZED = 401;
    public static final String SUNAUTHORIZED = "unauthorized";

    //402
    public static final int PAYMENTREQUIRED = 402;
    public static final String SPAYMENTREQUIRED = "payment_required";

    //403
    public static final int FORBIDDEN = 403;
    public static final String SFORBIDDEN = "forbidden";

    //404
    public static final int NOTFOUND = 404;
    public static final String SNOTFOUND = "not_found";

    //405
    public static final int METHODUNALLOWED = 405;
    public static final String SMETHODUNALLOWED = "method_unallowed";

    //406
    //
    //

    //407
    //
    //

    //408
    public static final int REQUESTTIMEOUT = 408;
    public static final String SREQUESTTIMEOUT = "request_timeout";

    //415
    public static final int INVALIDMEDIATYPE = 415;
    public static final String SINVALIDMEDIATYPE = "invalid_media_type";

    //418
    public static final int TEAPOT = 418;
    public static final String STEAPOT = "i_am_a_teapot!";

    //423
    public static final int LOCKED = 423;
    public static final String SLOCKED = "locked";

    //426
    public static final int ENCREQUIRED = 426;
    public static final String SENCREQUIRED = "encryption_upgrade_required";

    //428
    public static final int PRECONDITIONREQUIRED = 428;
    public static final String SPRECONDITIONREQUIRED = "precondition_required";

    //429
    public static final int TOOMANYREQUESTS = 429;
    public static final String STOOMANYEQUESTS = "too_many_requests";

    //500
    public static final int INTERNALERROR = 500;
    public static final String SINTERNALERROR = "internal_server_error";

    //501
    public static final int UNIMPLEMENTED = 501;
    public static final String SUNIMPLEMENTED = "not_implemented";

    //503
    public static final int UNAVAILABLE = 503;
    public static final String SUNAVAILABLE = "service_unavailable";

    //507
    public static final int INSUFFICIENTSTORAGE = 507;
    public static final String SINSUFFICIENTSTORAGE = "insufficient_storage";

    //511
    public static final int NETWORKAUTHREQUIRED = 511;
    public static final String SNETWORKAUTHREQUIRED = "network_authentication_required";

}
