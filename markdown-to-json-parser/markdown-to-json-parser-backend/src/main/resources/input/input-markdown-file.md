FORMAT: 1A
HOST: https://www.localbank.eu/webapi/environment/version/

# Erste Group - George WebAPI (new version 2.0)

# Group WebAPI Basics

This API is a HTTP-1.1 REST service that provides access to user and accounting data of Erste Bank customers. It is a general purpose API with no single predefined use in mind. The following requirements need to be met:

* highly secure, as sensitive user data is provided
* fine granular access/permissions (e.g. allow read-only on certain business objects, or different permissions for different clients on the same account, ...)
* low protocol overhead, as it is may be used on smart phones with potentially low bandwidth or high base latency
* low payload overhead (e.g. JSON instead of XML)
* no special-purpose calls for a business case, but atomic, reusable calls

## Server calls

Server calls are done via *https* by using one of the following HTTP methods:

Method  | Idempotent  | Change state  | Purpose
------- | ----------- | ------------- | ------------------------------------------------------------------
GET     | yes         | no            | select: read-only call (e.g. get account list)
POST    | no          | yes           | create: add a new resource (e.g. create a new transaction), call a function
PUT     | yes         | yes           | update: change an existing resource (e.g. set a message read, lock a card)
DELETE  | yes         | yes           | delete: remove an existing resource (e.g. delete a message)

Resources are all URIs that deliver business data (e.g. accounts, transactions, messages, ...) while functions are calls that may or may not return data but do not operate on business data (e.g. login).

All data provided by the client has to be in UTF-8 encoding. 

GET, PUT und DELETE calls are idempotent. It does not matter for the server state if this call is used only once or multiple times (with the same resource/payload). 
POST calls, usually creating new stuff, should not be repeated by clients, because they will result in duplicated data.
Successful DELETE calls result in a "HTTP 204 - No content" reply without payload or alternative HTTP 200 with mandatory signInfo object (based on local business/security decision if DELETE method should be signed or not) and the resource is being removed (immediately or after signing).

The server must support gzip'ed content and all clients are strongly recommended to request gzip'ed content via the HTTP header:

    Accept-Encoding: gzip


### Authentication and Authorization

This API is not responsible for authentication (i.e. validate if a user/client/principal is who he/she/it claims to be, e.g. by checking user&passwords) but only of authorization (i.e. validate if a user/client/principal can access a resource here and now). Therefore, it is based on a token security concept and only needs to check the validity of such tokens which a client provides for each call as proof it may access the requested data. 

As far as this API is concerned, this token is a "black box" which is part of each client request. The API provides the received token to a server-side verification library or service, and if the token turns out to be valid *for the specific requested call*, the business logic is executed. The API does not have to parse the token itself.

This API can work with any type of token, as long as the following assumptions are valid:

* The token already is in a format/encoding allowed to be passed as HTTP 'Authorization' header field.
* The token is maximum 1024 bytes in length (as it has to be transferred with each request, and on mobile phones every ms counts). However, average tokens should be much shorter (e.g. 256 bytes) to reduce upload traffic for each and every request.
* It is possible (for a library) to verify the validity of the token.
* It is possible (for a library) to derive the user (Verfueger) from the token.
* It is possible (for a library) to derive the **fine granular permissions** from the token. This includes a separate read and write permission down to an individual **business object level** (e.g. one specific account, message or template). However, a token should alternatively refer permissions in larger units, especially on entities the User owns, e.g. "all my accounts".

**Note:** OAuth 2.0 bearer token will be used, which encapsulate SAML permissions. This match the above requirements.

The token in request is sent by the client as HTTP `Authorization:` header:

    Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

The content of the token and how to generate/validate it is not part of this specification.

#### Authentication in file downloads in old browsers

In order to overcome the limitations in old browsers, some file download endpoints supports file downloads as a result of form post calls. These calls return the required file immediately. Authorization information is carried in the hidden field of the form post call. 


    <form action="/api/netbanking/my/cards/id/invoices/2014_02/pdf" method="post"  accept="application/pdf">
       <input type="hidden" name="access_token" value="<token>">
       <input type="submit" value="download" />
    </form>



#### Authentication in file uploads in old browsers

For limitations similar as in *File downloads in old browsers*, some upload endpoints support passing the authorization token in a hidden form field in multipart form posts.


    <form action="/api/backoffice/donation/ngos/<id>/qlogo" method="post" enctype="multipart/form-data" accept="application/json">
       <input type="hidden" name="access_token" value="<token>">
       <input type="file" name="file">
       <input type="submit" value="upload" />
    </form>

### Session Cookies

In theory, each server call could be stateless and only use the token to reconstruct state. However, this will probably result in a performance impact, can be a cost issue due increased backend/host requests, and also would render current load balancing infrastructure useless.

Therefore, each time an server call is done that does not provide a standard Java Session Cookie (`JSESSIONID`), the reply will contain a new such session cookie. It is in the interest of the client to re-provide this cookie with each follow up call to make use of server-side caching. If the cookie is not re-sent, the call still will work but a slower reply will be the result.

    Set-Cookie: JSESSIONID=Zw4LQndKc2ffHXM7r9NY9r2nBVbJytpnh3lt6p2R9cTQLFNz5CG5!-2028444187;


The server will store the last-provided authentication token in the server-side session upon the first call. If the token and the session ID still match on a successive call, than it is not necessary for the server to re-validate the token as long as it is not expired. If however the token does not match to the session ID, which means the client is suddenly providing a new token, the session is immediately destroyed and a new one is created based on the new provided token. The resulting new `JSESSIONID` is sent to the client for re-use in successive calls.

Session Cookies are valid for 2 minutes or the validity of the authentication token, whichever is *shorter*.

### Content types and languages

The client has to specify what type of content it expects via ordinary HTTP headers:

    GET /path/to/some/resource HTTP/1.1
    Host: www.example.org
    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0
    Accept: application/json
    Accept-Language: en-US


**Accept**: What type of content should be returned. Currently, only `application/json` is supported. This is also the default if no `Accept:` header is given.

**Accept-Language**: In case language-dependent content is in the reply (GUI texts, pre-formatted amounts or dates, ...), the language-dependent info of this language should be used by the server to translate/pre-format. If no `Client-Accept-Language:` is given, `en-US` is assumed. The server may ignore this field in case the user has a server-side language setting which overrules any request setting.

### Server side pagination

All calls that return lists support pagination. The client can request the content in pages, providing a `size` and a `page` number. Those are given as URL parameters like the following example:

    GET /path/to/some/resource?size=25&page=1


`page` count starts at zero, so 0 is the first page, 1 the second and so on. If `size` is given without any `page`, `page`=0 is assumed. 

There is no predefined `size` limit. If it is omitted, all records are returned in one large list. However, some calls might introduce a size limit due the fact that a certain backend would be overloaded by returning too many items in one call - in such a case, the individual call descriptions will state that clearly.

### Server side sorting

Some calls allow their output to be pre-sorted. This can be requested by giving the `sort` and `order` URI parameters, for example:

    GET /path/to/some/resource?sort=date&order=desc

Each call description below states if and what fields it supports to sort. The optional `order` can be either `asc` or `desc` (case insensitive), with `asc` as default. If no `sort` is given, a random order has to be assumed that can change between calls.

Sorting multiple fields at the same time is possible by comma-separating the fields and their corresponding `sort` orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding `order` entries are considered to be `asc`. For example:

    GET /path/to/some/resource?sort=date,title,name&order=desc,,desc

For paginated output, sorting is done before pagination, so the whole list not only one single page is sorted. Resources/Lists requested without giving any sorting parameters should be considered to be in random order.

Sorting by optional attribute in response should be done that way that first will be provided items with value (in requested order) and then items without attribute value (null or missing attribute value). 
 
### Alternate header names

In some cases it might not be possible for an application to change certain HTTP headers. For example, a website calling this API via JavaScript from within an older browser might not be able to override the `Accept:` or `Accept-Language:` header. In such a case corresponding alternate header names prefixed with `Client-` can be used. If the server receives such headers, they take precedence over any non-prefixed header that might also be included in the call.

    Client-Accept: application/json
    Client-Accept-Language: en-US
    Client-Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
 
Note: This is not a complete list. All HTTP headers mentioned in this document can be prefixed. 

### Last modified

Some calls support the HTTP last modified mechanism (this is stated in those calls that do). For such calls, the client may provide a standard RFC-complaint HTTP `If-Modified-Since` header, and resources delivered by that calls include a `Last-Modified` header. 

    GET /path/to/some/resource HTTP/1.1
    Host: www.example.org
    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0
    If-Modified-Since: Wed, 19 Oct 2005 10:50:00 GMT

If the resource was not modified since the given timestamp, a HTTP `304` is returned (means "not modified"). If the resource was updated on this time or thereafter, the resource is returned. Note: this does not do delta handling. If `If-Modified-Since` is used on a list, all or none items are returned, not only the part of the list that is newer.

Since HTTP's `If-Modified-Since` only supports seconds, but data on the server might be more precise, the following mechanism is in place: If a resource has a last modified date of a fraction of a second, and a client requests this resource within the very same second, the server returns the resource as `Last-Modified` with the second *rounded down*. E.g. for a resource that has a timestamp of 12:30:00.123, 12:30:00 is returned. If the request is done at a later point in time, the `Last-Modified` second is to be *rounded up*. This is done to avoid that the client might miss an update due the higher precision of time on the server than in the HTTP headers,  at the cost that in the - rare - event, that if a resource is queried at the very same second it was changed, it might be queried twice due the rounded down timestamp.

As an alternative to `If-Modified-Since`, the API also supports a `X-ebsapi-Last-Modified`/`X-ebsapi-If-Modified-Since` header pair. Those accept an XML datetime format (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`) with millisecond precision.

    GET /path/to/some/resource HTTP/1.1
    Host: www.example.org
    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:16.0) Gecko/20100101 Firefox/16.0
    X-ebsapi-If-Modified-Since: 2013-06-05T16:42:22.709Z

 
### Batch Calls (todo)

Sometimes a client wants to combine individual calls to one bigger call, so the amount of calls and the HTTP overhead is lower (e.g. netbanking app after login needs accounts, templates and messages).

There is no dedicated batch call mechanism. Clients are asked to use [HTTP 1.1 Pipelining](http://en.wikipedia.org/wiki/HTTP_pipelining) for that, and the server is supposed to support that.

## Server request

### JSON

In case of POST / PUT / DELETE requests business data is accepted as JSON data (if it is not binary data). See http://en.wikipedia.org/wiki/JSON for details on JSON.

Empty arrays `[]` and `null` values must be omitted from input data (request payload).

### Error handling

JSR-303 validation is applied to the provided JSON input data according to the appropriate data model.

## Server reply

All payload data returned by the server is in UTF-8 encoding.

If *no error* occurred, the server replies with a HTTP 200 status code and the payload contains the JSON with all relevant data, or a HTTP 204 status code and no body/payload if the call does not produce data. For *logical errors* like empty fields, field parsing errors, missing mandatory fields etc. the server replies with HTTP 400 and a JSON error object (see section below). For *framework and meta-errors* (authentication failed, wrong URL, ...) the corresponding HTTP Error codes are used and the payload contains a JSON error object.
If a resource can't be found due to inactive products in BackEnd or wrong id ( e.g. GET /netbanking/my/contracts/buildings/{id} ) the server replies with HTTP 404 and a JSON error object describing the reason (e.g. ID_NOT_FOUND or PRODUCT_NOT_ACTIVATED) has to be returned.

Code  | Purpose
----- | ---------------------------------------------------------------------------
200   | indicates no technical error happened
204   | like 200, but no body/payload will be sent back
400*  | logical error in parsing request (e.g. ID does not exist, mandatory field missing, ...)
403*  | user not logged in, invalid token, etc.
404*  | this kind of resource does not exist
415   | unsupported media type (Accept/Accept-Language unsupported)
500   | internal error happened (a Java exception happened)
503   | service unavailable (The server is currently unavailable, because it is overloaded or down for maintenance. Generally, this is a temporary state)

**Legend:** * = returns JSON error object

### JSON

Business data is returned as JSON data (if it is not binary data like images). See *http://en.wikipedia.org/wiki/JSON* for details on JSON.

JSON data has no root object and it should look like

    {
    ... do this ...
    }

but not like this

    "objectname" : {
    ... don't do this ...
    }


Undefined or unused optional fields are omitted in the JSON output and can't be delivered as `null` nor as empty strings. If the distinction between empty and not available is necessary, of course an empty string is still a valid response.

Empty arrays `[]` and `null` values must be omitted from output data (response payload). 

### Error handling

WebAPI Error Handling concept covers following requirements:

- Client side/Business requirements:
    - multi-language support - one error must be presentable in multiple languages
    - using parameters - error message should handle dynamic parameter values (e.g. “Your payment order is over limit.
Current limit is XX EUR, you entered YY EUR”)
    - multiple app support - the same error code can have different messages in different apps (e.g.
different error message in mobile/desktop apps)
    - multiple client segment support - the same error code can have different messages for different
types of customers (e.g. “Hey bro, you’re asking too much” vs “Your payment order is over
limit”)

- Backoffice/Operation needs:
    - unique error codes - one error cause matches exactly one error code
    - single point of maintenance for all apps - error codes and messages are manageable in one
place, to prevent misleading interpretations across apps. Build process or distribution process
of apps should involve usage of this point somehow
    - understandability - support teams should easily identify error cause, both from support tools
and from app logs

- Technical requirements:
    - We need to handle messages for HTTP status codes
    - Frontend sometimes reacts on exact value of error code - this complicates changes and should
be solved somehow
    - Cross-country - there should be common errors used for common Group API, but also locally
specific errors
    - Severity - application should have some common way how to react to severity of error
    - Unparametrized message for errors - most of BEs doesn’t support parametrization, so there
should always be generic message without parameters for any error

Solution covering all these requirements is "Common error codetable" - One place for all errors with following attributes:

Attribute             | Meaning                                             | Note
----------------------|-----------------------------------------------------|----------
errorCode             | Unique character error code, e.g. `LIMIT_EXCEEDED`  | Locally specific errors should have prefix, e.g. `CZ_ERRLIMIT`
errorDescription      | Human-readable error description, not localized. Its intended audience is application support, should be used in app logs together with error code  | This description SHOULD NEVER be sent to frontend/displayed to user. It is meant only as a guide for support staff and maintainer of error codetable.
application           | Identification of application or empty for default  | Error messages might be application specific. There should always be default message.
language              | Language of message  | Language for particular error message - one row in Error codetable.
message               | Message text with placeholders for parameters       | Placeholders for parameters will be named, so that different position can be used for the same parameter in different languages.
parameterNames        | List (comma-separated) of parameters names that can be used in message.  | This is only information for support to indicate, which parameters can be used to tailor resulting message.
parameterTypes        | List (comma-separated) of parameters types that can be used in message.  | This information can be used as formatting hint for application to construct error message.
customerSegment       | Specifies segment of customer or empty for default. Different types of customers may require different way of communication.  | There should always be default message.
unparametrisedMessage | General message text without parameters             | Some BE cannot provide parameters for error messages. This message should be used when no parameters are available.
severity              | Severity parameter, indication for frontend how it should react. Possible values: `INFO`, `WARNING`, `BUSINESS_ERROR`, `TECHNICAL_ERROR`, `FATAL_ERROR` | Proposed George FE behavior:<br> 1. George FE should display message of severity=`INFO` like popup with OK button<br> 2. George FE should display message of severity=`WARNING` like popup with OK (allow to continue in flow) and Back (return back in flow) buttons<br> 3. George FE should display message of severity=`BUSINESS_ERROR` like popup with Back (return back to, not allow to continue in flow) button<br> 4. George FE should display common application error message and navigate to George homepage after receiving severity=`TECHNICAL_ERROR`<br> 5. George FE should display common fatal error message and log off client from George FE after receiving severity=`FATAL_ERROR`

#### Frontend Usage of Common Error Codetable

Copy of error codetable should be included in any application using this concept. This can be achieved either by build process (suitable especially for web applications), or by some service providing its contents (e.g. mobile app will have a copy included at its build and then the list can be regularly updated by calling this service).
Frontend only receives error code and parameters names/values (returned in the error wrapper object), error message is built on fronted using error codetable and parameters values.

#### WebAPI error communication

Errors are given as codes of `WORDS_AND_UNDERSCORES`. It is the responsibility of the FE client application to convert them using the language of the user and data from Error Codetable.

If a call fails with one of the 40x HTTP codes, the payload will contain a top-level `error` JSON array. It contains one entry per logical error that describes more in detail what happened. Errors come in two flavors, as following example output shows:

    "errors": [
        {
            "error":"SOME_ERROR_CODE"           // error code
        },
        {
            "error":"ANOTHER_ERROR_CODE",       // error code
            "scope":"account.amount.currency"   // optional attribute indicating JSON parameter name that caused the error. Provided by WebAPI logic/validation when applicable
        },
        {
            "error":"OTHER_ERROR_CODE",         // error code
            "parameters": {                     // list of named parameters for current message
                "AMOUNT_ENTERED": 10000,        // named parameter “AMOUNT_ENTERED” and its value
                "CURRENCY": "EUR",              // named parameter “CURRENCY” and its value
                "LIMIT": 500,                   // named parameter “LIMIT” and its value
            }
            "scope":"orders[3].amount.value"    // scope can use array index for identification of particular field
        }
    ]

Simple errors only contain an *error* key with some textual error code, for example `SESSION_EXPIRED`. Validation errors contain in addition to the *error* key a *scope* key that refers to the input element that cause this error. The scope is fully qualified within the JSON input using dots, array items are qualified via `[123]`.

For HTTP status codes errors (e.g. PUT resource returning 404 - Not Found), error code will be returned in “xAPI_ERROR_CODE” header attribute. All message parameters will be also returned as header attributes, prefixed by “xAPI_” (so that AMOUNT_ENTERED error attribute will be returned as “xAPI_AMOUNT_ENTERED”).

The following common error codes are defined:

Error code              | Purpose
----------------------- | --------------------------------------
`TOKEN_NOT_ALLOWED`     | The provided token does not allow this call.
`TOKEN_EXPIRED`         | The provided token is expired.
`TOKEN_INVALID`         | The provided token not a valid token (syntax error).
`FIELD_NOSORT`          | The field (given in the "scope" field of the error) can not be used for sorting (does not exist, does not support sorting).
`RESOURCE_NOSORT`       | Resource does not support sorting.
`INTERNAL_SERVER_ERROR` | A technical exception occurred.

If a call can produce additional errors, they are listed in each call separately.

#### WebAPI error communication

Different backends may have different error codes for the same error. Mapping mechanism should exist to map this codes to unified error codetable; implementation of this mechanism is beyond the scope of this document. Proposed solution in CS is to include this mechanism into WebAPI business logic and maintain mapping form BE to common error code in one place, probably in RDS. This will however be locally specific.

### Pagination

If a request requires paginated output (see Server calls/Server side Pagination above), the reply will contain in addition to the array with a page's items the following fields:

    {
        "pageNumber": (INTEGER),
        "pageCount": (INTEGER),
        "nextPage": [INTEGER],
        "pageSize": (INTEGER),
        "page": [
            { ...item...},
            { ...item...},
            { ...item...}
        ]
    }

**pageNumber**: The page number of provided list items from total query result set.

**pageCount**: The total count of pages as result of calculation using the requested page size and total records in query result set.

**nextPage**: Optional page number of the next page, if exists the nextPage value equals pageNumber+1, if current pageNumber is the last page of result set then nextPage field is not provided.

**pageSize**: The page size used. Either the number supplied by the client in request is returned here, or in case there is a page size limit for call and its value is less than the requested one, the actual size is returned.

**page**: The page's items. Note that `page` can be any name, e.g. `accounts`, `cards`, `transactions`.

## Naming Conventions

### JSON Fields

* Field names are in English.
* Field names are written camelCase.
* The first letter is always lowercase. 
* Arrays have plural names and end with `s` or `es`. 
* Abbreviations are always written with first letter uppercase (IBAN -> Iban).
* The main ID of an object is called just `id`.
* All other referred ids are given via the name of the referred object plus `Id'.
* Field names do not have a prefix of the object type, so it's not `accountName` but just `name`.

Examples:

    {
        "id": 1,
        "anotherId": 5,
        "remoteIds": [ 7, 8, 9 ],
        "flyingWalruses": [ "walrus 1", "walrus 2" ],
        "iban": "some IBAN",
        "someOtherIban": "other IBAN",
        "flags": [ ... array ... ]
    }


### Error codes

* Error codes are English.
* Error codes are given all uppercase.
* Underscores separate multi-word codes. 
* Error codes are self-explanatory descriptions, not internal codes or strings without obvious meaning. 
* Error codes follow a NOUN or NOUN_ADJECTIVE pattern.
* Error codes are *not* UI-Texts and need a mapping first.
* Error codes may get long if that helps to increase clarity.

Examples:

    ID_NOT_FOUND
    SYNTAX_ERROR
    DAILY_LIMIT_EXCEEDED
    WALRUS_FLEW_AWAY
    CLARITY_INCREASED_BY_VERY_LONG_ERROR_CODES

### Flags

Entries for the (FLAG) datatype (definition below) follow the same conventions as JSON-Fields. In addition the following rules apply:

* Flags follow a "noun" or "nounAdjective" pattern.

Examples:

    defaultAccount
    domesticPaymentAllowed


## STS / Federated Login

Security Token Service functionality provided by Federated Login  solution is described in FL documentation.

 ENV   | URL                                            | Testuser
------ | ---------------------------------------------- | --------------
 ENTW  | https://logind.imcplus.net/sts/clients         | 101467721 / BHKWXESwDV
 FAT   | https://login.fat.sparkasse.at/sts/clients     | 304844216 / testen123
 PROD  | tbd                                            | 

## API URLs

Prefixes in URL are used to differentiate the environment version of API. Every local country implementation should define it as following example from AT:

 ENV   | HOST
------ | ----------------------------------------------
 ENTW  | tbd
 FAT   | https://api.fat.sparkasse.at/rest/
 UAT   | https://api.uat.sparkasse.at/rest/
 PROD  | tbd

### Postman URLs

All API endpoints below can be called via:

    {HOST}/rest-web/api/{ENDPOINT}?access_token={TOKEN}


For the `HOST`, see table above. The `ENDPOINT` is the individual call, e.g. `/netbanking/my/accounts`. The `TOKEN` is the access token obtained from the Federated Login STS service.

## API Calls

The following calls are handled by the API, which might or might not be deployed at the same servers as the authentication BackEnd used by the client to obtain the necessary permission/tokens.

 ENV   | URL for AT
------ | -------------------------------------------------------------
 ENTW  | https://mobileappsd.imcplus.net/rest/
 FAT   | https://api.fat.sparkasse.at/rest/
 PROD  | https://api.sparkasse.at/rest/

All Client-based calls require a token issued by the authentication system to be present in the HTTP headers (see Authentication & Security above). Only anonymous API calls (like press releases or exchange-rates) may be used without a token.

Design guidelines for this API calls are following:

* URI-Path starting with `/my/...` refer to the owner of the authorization token.
* URI elements with plural nouns refer to lists of objects, e.g. `/my/accounts`. They support pagination and very often sorting.
* To get a single object from a list, you append the ID to the list's URI, e.g. `/my/accounts/1234`.
* If a summary/short version of a list exists (e.g. with only the IDs), `digest` is appended to the list's URI, e.g. `/my/accounts/digest`.
* JSON fields should be named to the point and not prefixed. A `transaction` object, for example, should have an `id` and not a `transaction_id`, a `limit` not a `transaction_limit` and such. If fields only make sense when grouped, a sub-object should be introduced. So instead of an `amount_value` and `amount_currency`, the `transaction` should have an `amount` field holding an object, that encapsulates a `value` and a `currency`.
* JSON fields and objects use CamelCase names.

As a general principle the API should not provide (localized) GUI texts. All messages are given as status/error codes or text/ENUM codes.


## API Versioning

It is important to present API version of the endpoint, which is used in WebAPI in user call. If an endpoint is marked to use certain version of API then please use the given version when calling the given endpoint. If no version is used in request call, then the most current version of API is used.


# Group Data Types

The individual call descriptions below reuse the following common data types. (The uppercase word in parentheses will be used in the individual call descriptions.) A field specifying a datatype in round brackets is mandatory, e.g. "(TEXT)", a field in square brackets is optional, e.g. "[TEXT]". If a datatype has a maximum length, the length is appended to the datatype, e.g. (TEXT100). If a field has a minimum and a maximum, both values are given and separated via a minus, e.g. (TEXT6-32).

When specifying arrays, the array is shown as a single line with three dots. The type of the array is defined by listing the first entry. The use of round or square braces around that first entry defines if the array itself is optional or not. Mandatory array must have at least one item. Empty arrays are omitted from the output.

    {
        "mandatoryArray": [ (SOMETYPE), [SOMETYPE] ],
        "optionalArray": [ [SOMETYPE], [SOMETYPE] ]   
    }  

If data types (see below) define error codes, those have to be returned whenever an object using such a field is POSTed or PUTed. In addition, the following generic errors can occur:

Error code        | Purpose
----------------- | ----------------------------
`FIELD_MISSING`   | The field is missing in the JSON input.
`FIELD_TOO_LONG`  | The field maximum length is exceeded.
`FIELD_TOO_SHORT` | The field minimum length is not given. 

If one check fails, a HTTP 403 plus an error object is returned, with the `scope` defining the source/field of the error.


## DataType:Text (TEXT)

Text is given in UTF-8 encoding. 

| Level | Datatype name           | Type        | Mand/Opt |  Pattern |            Constant                 |
|-------|-------------------------|-------------|----------|----------|-------------------------------------|
| 0     | TEXT                    | string      |          |          |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | ----------------------------
`TEXT_INVALID`     | Not a string.


## DataType:HTML-Fragment (HTML)

Text with HTML tags for formatting.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern |            Constant                 |
|-------|-------------------------|-------------|----------|----------|-------------------------------------|
| 1     | HTML                    | TEXT        |          |          |                                     |


## DataType:Integers (INTEGER)

Integers are variable-length numbers (positive or negative). They do not have an upper size limit (like 32 or 64 bit).

| Level | Datatype name           | Type        | Mand/Opt |  Pattern |            Constant                 |
|-------|-------------------------|-------------|----------|----------|-------------------------------------|
| 0     | INTEGER                 | number      |          |          |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | ----------------------------
`INTEGER_INVALID`  | Not an integer (float, ...).


## DataType:Float (FLOAT)

The float type is as four-byte, single-precision, floating-point number. It represents a single-precision 32-bit IEEE 754 value. 
The float type can represent numbers as large as 3.4E+38 (positive or negative) with an accuracy of about seven digits, and as small as 1E-44.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern |            Constant                 |
|-------|-------------------------|-------------|----------|----------|-------------------------------------|
| 0     | FLOAT                   | number      |          |          |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | ----------------------------
`FLOAT_INVALID`    | Not a floating-point number


## DataType:Boolean (BOOLEAN)

The boolean type has just two values: `true` or `false`.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern                                   |            Constant                 |
|-------|-------------------------|-------------|----------|--------------------------------------------|-------------------------------------|
| 0     | BOOLEAN                 | boolean     |          |                                            |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | -----------------------------------------
`BOOLEAN_INVALID`  | Not a boolean values true/false provided


## DataType:Universally unique identifier (UUID)

A 36-digit string containing a standard [universally unique identifier](http://en.wikipedia.org/wiki/Universally_unique_identifier).

    "uuid": "550e8400-e29b-41d4-a716-446655440000"

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | UUID                    | TEXT36      |          |                        |                                     |

### Checks & Error Codes

Error code     | Purpose
-------------- | ----------------------------
`UUID_INVALID` | Not a correct uuid string.


## DataType:Currency (ISO4217)

Currency is in ISO 4217 format (3 capital letters code).

    {
        "currency":"EUR"           // (ISO4217)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ISO4217                 | TEXT3       |          | [A-Z][A-Z][A-Z]        |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | ----------------------------
`CURRENCY_UNKNOWN` | Not an ISO 4217 currency.


## DataType:Country Codes (ISO3166)

Country codes are in ISO 3166-1 format, subtype ALPHA-2. This means two letters in uppercase. 

    {
        "country":"AT"           // (ISO3166)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ISO3166                 | TEXT2       |          | [A-Z][A-Z]             |                                     |

### Checks & Error Codes

Error code             | Purpose
---------------------- | ----------------------------
`COUNTRY_CODE_UNKNOWN` | Not an ISO 3166 country code.


## DataType:Hashes (HASH)

All hashes are generated as SHA256.

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | HASH                    | TEXT        |          |                        |                                     |


## DataType:Amounts (AMOUNT)

Amounts are objects that include the value, the precision and the currency. 

    "amountExample" : {
        "value":12345678900,       // (INTEGER)
        "precision":2,             // (INTEGER)
        "currency":"EUR"           // (CURRENCY)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | value                   | INTEGER     | M        |                        |                                     |
| 1     | precision               | INTEGER     | M        |                        |                                     |
| 1     | currency                | ISO4217     | M        |                        |                                     |

### Checks & Error Codes

Error code          | Purpose
------------------- | ---------------------------------------------
`INVALID_PRECISION` | Precision not an integer or not >= 0.
`VALUE_INVALID`     | Value is not an integer.

Also, "currency" has to be checked according to (CURRENCY) rules.


## DataType:Date (DATE)

Date format without time (YYYY-MM-DD).

    {
        "aDate":"2011-07-14"  // (DATE)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern                                                        |            Constant                 |
|-------|-------------------------|-------------|----------|-----------------------------------------------------------------------|-------------------------------------|
| 1     | DATE                    | TEXT        |          |                                                                       |                                     |


## DataType:Dates (DATETIME)

Dates are returned as ISO 8601 dates without miliseconds (fraction of seconds) with mandatory timezone and are used for both dates, times and timestamps. 
Format of the datetime is YYYY-MM-DDThh:mm:ssTZD where:
    YYYY = four-digit year
    MM   = two-digit month (01=January, etc.)
    DD   = two-digit day of month (01 through 31)
    hh   = two digits of hour (00 through 23) (am/pm NOT allowed)
    mm   = two digits of minute (00 through 59)
    ss   = two digits of second (00 through 59)
    TZD  = time zone designator (Z for UTC or +hhmm or -hhmm for offset from UTC)
    
This profile defines two ways of handling time zone offsets:
- Times are expressed in UTC (Coordinated Universal Time, former GMT), with a special UTC designator ("Z" knows as Zulu time).
- Times are expressed in local time (preferred way), together with a time zone offset in hours and minutes to UTC. A time zone offset of "+hhmm" indicates that the date/time uses a local time zone which is "hh" hours and "mm" minutes ahead of UTC. A time zone offset of "-hhmm" indicates that the date/time uses a local time zone which is "hh" hours and "mm" minutes behind UTC.

    {
        "aDate":"2011-07-14T19:43:37+0100",  // (DATETIME)
        "yetAnotherDate":"2001-10-26T19:32:52Z"  // (DATETIME)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | DATETIME                | TEXT        |          |                        |                                     |


All dates returned by the API are in the corresponding time zone a record/data object is for. For Austrian dates (e.g. transfer timestamps) this means Central European Standard Time (CEST). Austria is UTC+1 in winter and UTC+2 in summer, so times usually use offset `+0100` or `+0200`. 
Should, at a later time, the API be applied to other countries, the corresponding time zone for a given data object has to be used. 

Note: Returning dates as UTC is discouraged because a client would not know what the intended target time zone is that a date has to be displayed in (e.g. an Austrian transaction still has to be shown with the correct Austrian execution time, even if the caller is using the API from e.g. the USA.)

All dates provided by the client (e.g. a date for a future payment order) can be in any timezone. They will be converted to CEST on the server and this CEST version is used for all further processing - including storing or returning this now converted date to the client.

### Checks & Error Codes

Error code         | Purpose
------------------ | ---------------------------------------------
`DATE_INVALID`     | String does not contain a valid ISO 8601 date string.
`TIMEZONE_MISSING` | Datetime without a timezone information was provided.
`TIME_NOT_ZERO`    | The client should have provided a time, that (after CEST conversion) should have been 00:00:00, but isn't.


## DataType:Enums (ENUM)

Variables where the actual value is one of a predefined list (a.k.a. enums). The range of values are given as comma-separated list right in the datatype definition, e.g. (ENUM:GI,WP,SP,KA). Domain values are keys used in Domain Database, that provides company-wide reusable keys, short- and long-texts for them.

    "some_domain_value":"SP"     // (ENUM:GI,WP,SP,KA)

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ENUM                    | TEXT        |          |                        |                                     |

### Checks & Error Codes

Error code      | Purpose
--------------- | ---------------------------------------------
`ENUM_UNKNOWN`  | Given element is not part of this (ENUM).


## DataType:Flags (FLAGS)

Flags/Tags/Roles that an object can have. To avoid adding a lot of case-specific boolean values, things (that an object is or not) are represented by flagging the object. Flags are an array of strings representing one aspect, e.g. one permission, one attribute or one role. The existence of a certain string in a Flag-List can be considered to be a "true" on this aspect, the absence of a certain string as a "false". The possible flags are listed on a case-by-case basis at each data type/call as comma-separated list within the brackets. 

    "flags": [
        "hidden", "owner"   // (FLAGS:hidden,owner,unused1,unused2)
    ]

Empty flag arrays must be omitted.

| Level | Datatype name           | Type                 | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|----------------------|----------|------------------------|-------------------------------------|
| 1     | FLAGS                   | ARRAY of TEXT        |          |                        |                                     |

### Checks & Error Codes

Error code      | Purpose
--------------- | ---------------------------------------------
`FLAG_UNKNOWN`  | Given element is not possible/allowed as (FLAG).


## DataType:Features (FEATURES)

Features that an object can have or is capable of. To avoid adding a lot of case-specific attributes, Features are an array of strings representing one aspect, e.g. one feature of object. The existence of a certain string in a Feature-List can be used on FE to allow some functionality for object. The possible features are listed on a case-by-case basis at each data type/call as comma-separated list within the brackets. 

| Level | Datatype name           | Type                 | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|----------------------|----------|------------------------|-------------------------------------|
| 1     | FEATURES                | ARRAY of TEXT        |          |                        |                                     |

### Checks & Error Codes

Error code         | Purpose
------------------ | ---------------------------------------------
`FEATURE_UNKNOWN`  | Given element is not possible/allowed as (FEATURE).


## DataType:Basic bank account numbers (BBAN)

BBAN (local Basic Bank Account Number consisting of 1-30 characters). 

| Level | Datatype name           | Type        | Mand/Opt |        Pattern                          |            Constant                 |
|-------|-------------------------|-------------|----------|-----------------------------------------|-------------------------------------|
| 1     | BBAN                    | TEXT30      |          |                                         |                                     |


## DataType:International bank account numbers (IBAN)

Based on ISO 13616-1:2007. A valid IBAN consists of all three of the following components: Country Code (2 capital letters), check digits (2 digits) and BBAN (local Basic Bank Account Number consisting of 1-30 characters). 

    "iban": "AT896000000005544815"

| Level | Datatype name           | Type        | Mand/Opt |        Pattern                          |            Constant                 |
|-------|-------------------------|-------------|----------|-----------------------------------------|-------------------------------------|
| 1     | IBAN                    | TEXT        |          |  [A-Z]{2}[0-9]{2}[0-9a-zA-Z]{1,30}      |                                     |

### Checks & Error Codes

Error code      | Purpose
--------------- | ---------------------------------------------
`IBAN_INVALID`  | The given IBAN is not syntactically correct.


## DataType:Business Identifier Code (BIC)

BIC code (also know as SWIFT ID/code) standard format (based on ISO 9362) has 8 or 11 characters, made up of: 

- 4 letters: Institution Code or bank code
- 2 letters: ISO 3166-1 alpha-2 country code
- 2 letters or digits: location code
- 3 letters or digits: branch code, optional (possible default 'XXX' for primary office)

Example:

    "bic": "OPSKATWW"

| Level | Datatype name           | Type        | Mand/Opt |  Pattern      |            Constant                 |
|-------|-------------------------|-------------|----------|---------------|-------------------------------------|
| 1     | BIC                     | TEXT        |          |               |                                     |

### Checks & Error Codes

Error code      | Purpose
--------------- | ---------------------------------------------
`BIC_INVALID`   | The given BIC is not syntactically correct.


## DataType:Bank Code (BANKCODE)

Local bank code used in local bank clearing system, e.g. 5-digit bank code in AT, 4-digit bank code in CZ, SK.

    "bankCode": "20111"     //Erste Bank der oesterreichischen Sparkassen AG
    "bankCode": "0800"      //Ceská sporitelna, a.s.
    "bankCode": "0900"      //Slovenská sporitelna, a.s.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern      |            Constant                 |
|-------|-------------------------|-------------|----------|---------------|-------------------------------------|
| 1     | BANKCODE                | TEXT        |          | [0-9]{4,5}    |                                     |

### Checks & Error Codes

Error code           | Purpose
-------------------- | ---------------------------------------------
`BANKCODE_INVALID`   | The given local BANKCODE is not correct.


## DataType:Account number (ACCOUNTNO)

Account number consists of IBAN (IBAN plus optional BIC) identification or local account number (BBAN) plus mandatory bank code plus optional country code. 
This is due to fact, that using of IBAN format was not adopted in all the Erste group countries. Some applications of ACCOUNTNO are restricted to one of the two flavors only. F.i. when posting new SEPA payment orders sender and receiver accounts must be specified by IBAN format only, or when posting new Domestic payment in CSAS BBAN format should be used.  However when requesting existing transactions the receiver account may be returned in format BBAN or IBAN (for SEPA payments). Therefore the ACCOUNTNO object may in some cases only contain IBAN, only local BBAN, or both.

    "accountNoExample" : {
        "iban":"AT896000000005544815",    // [IBAN] IBAN or BBAN must be provided
        "bic":"OPSKATWW",                 // [BIC] optional
        "number": "5544815",              // [BBAN] BBAN (or free account format for SWIFT when IBAN is not used) or IBAN must be provided
        "bankCode": "60000",              // [BANKCODE] mandatory when BBAN is used
        "countryCode": "AT"               // [TEXT2] mandatory for international orders
    }

Account attribute *number* and *bankCode* are not padded with zeros.
Account attribute *number* in CZ consists of optional account prefix (TEXT6) and account number (TEXT10) and format is "XXXXXX-NNNNNNNNNN" if prefix is not null or "000000". If prefix is not provided then format is "NNNNNNNNNN" without leading zeros. 

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | iban                    | IBAN        |    C     |                        |                                     |
| 1     | bic                     | BIC         |          |                        |                                     |
| 1     | number                  | BBAN        |    C     |                        |                                     |
| 1     | bankCode                | BANKCODE    |          |                        |                                     |
| 1     | countryCode             | ISO3166     |          |                        |                                     |


## DataType:Base64File (BASE64FILE)

Represents a binary file with the (base64) encoded content and content type.

    {
        "contentBase64": (XS:base64Binary), // Encoded file content (base64Binary is defined in XML Schema)
        "contentType": (TEXT)              // MIME type, e.g., "image/png", "image/gif"
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | contentBase64           | TEXT        |          |                        |                                     |
| 1     | contentType             | TEXT        |          |                        |                                     |


## DataType:SignInfo (SIGNINFO)

Represents signing information for the requested order.

    {
        "state": (ENUM:OPEN,NONE), // OPEN: Order should be signed. NONE: Order has been executed without signing.
        "signId": (STAMP24ID)      // Available when state is OPEN. Used for next signing calls.
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | state                   | TEXT        |          |                        |                                     |
| 1     | signId                  | TEXT        |          |                        |                                     |


## DataType:Id (ID)

Holds an id from a created entity.

    {
        id: (STAMPID)
    }

| Level | Datatype name           | Type        | Mand/Opt |  Pattern      |            Constant                 |
|-------|-------------------------|-------------|----------|---------------|-------------------------------------|
| 1     | ID                      | TEXT100     |          |               |                                     |


## DataType:Database Stamp Id (STAMPID)

Represents a (TEXT100) id

    eg:  db1fa43e-07f6-8240-8330-ed20c8307240

| Level | Datatype name           | Type        | Mand/Opt |  Pattern      |            Constant                 |
|-------|-------------------------|-------------|----------|---------------|-------------------------------------|
| 1     | STAMPID                 | TEXT100     |          |               |                                     |


## DataType:Email (EMAIL)

Represents an E-mail address.

    {
        "emailExample": "john.doe@test.com"  // (EMAIL)
    }

| Level | Datatype name           | Type        | Mand/Opt |  Pattern       |            Constant                 |
|-------|-------------------------|-------------|----------|----------------|-------------------------------------|
| 1     | EMAIL                   | TEXT        |          | ^(.+)@(.+)$    |                                     |


# Group Banking Services
Banking Services API consist of:
- initial set of endpoints needed for George Overview page (Release 0)
- endpoints for George Release 1 (defined for CSAS)
- endpoints for George Release 2 (defined for CSAS)
- endpoints for George Release 3 (defined for CSAS)
- endpoints for George Release Beta (defined for SLSP)


# Group Authorization
Authorization token related services of this API.

## AuthorizationToken [/netbanking/auth/token/invalidate]
Authorization token (OAuth 2.0 Bearer) is provided by Federated Login solution after successful login to application (George FE).

### Invalidate authorization token [DELETE]
Removes the session of the token provided. Also deletes the token from the STS by calling {sts-url}/sts/oauth/tokens/{TOKEN}.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
No payload is provided.

Description of DELETE resource attributes:
No payload.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | token    | The provided token does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 204 (application/json)


# Group Profiles
Profile-related resources of *Banking Services API*.

## Profile [/netbanking/my/profile]
Resource Profile represents basic "netbanking" configuration of user/client profile.

Description of **Profile** resource/type attributes: 

| Level | Attribute name          | Type/Enum   | Mand/Opt | Editable | Attribute description                                                                                                                                                                 | Expected values/format                                                      |
|-------|-------------------------|-------------|----------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| 1     | customerId              | TEXT        | M        | No       | Internal ID of customer                                                                                                                                                               | e.g. ABC12345                                                               |
| 1     | firstname               | TEXT20      | M        | No       | Customer first name                                                                                                                                                                   |                                                                             |
| 1     | lastname                | TEXT35      | M        | No       | Customer surname, last name                                                                                                                                                           |                                                                             |
| 1     | salutation              | TEXT50      | O        | No       | Customer name used for salutation in particular language based on request header language attribute (used in Czech Republic, Croatia, Serbia where salutation name differs)           |                                                                             |
| 1     | individualGreeting      | TEXT140     | O        | No       | Individual greeting message set by customer (currently not used by George FE)                                                                                                         |                                                                             |
| 1     | alias                   | TEXT35      | O        | No       | Alias of user name. Currently not used in George - for future it is going to be used in another apps, therefore it is stored in the BackEnd.                                          |                                                                             |
| 1     | instituteId             | INTEGER     | O        | No       | Institute ID (defined for each branch in AT, mandatory only for AT). George FE uses it only for showing logo if the value is provided.                                                |                                                                             |
| 1     | offlineKey              | TEXT        | O        | No       | OfflineKey was used by QC - unused in George FE                                                                                                                                       |                                                                             |
| 1     | supportCategory         | ENUM        | O        | No       | Optional value defining the type of support the user gets (if the backend provided value is unknown or empty, the field will not be delivered at all). Currently unused in George FE. | ENUM values: [PREMIUM, INDIVIDUAL, TOP, CLASSIC, STANDARD]                  |
| 1     | marketingInfoAcceptance | ENUM        | M        | Yes      | Flag if customer accepted providing personal data for marketing purpose (§107 telecommunication act in AT)                                                                            | ENUM values: [ACCEPTED, NOT_ACCEPTED, UNKNOWN]                              |
| 1     | gender                  | ENUM        | M        | No       | Customer gender                                                                                                                                                                       | ENUM values: [MALE, FEMALE, UNKNOWN]                                        |
| 1     | dateOfBirth             | DATE        | O        | No       | Optional date of birth of customer.                                                                                                                                                   | ISO date format: YYYY-MM-DD                                                 |
| 1     | lastlogin               | DATETIME    | O        | No       | Optional date and time of the last login of customer. Common last login for all client applications - George, QC, etc.                                                                | ISO dateTime format: YYYY-MM-DDThh:mm:ssZ                                   |
| 1     | productPrivileges       | FLAGS       | O        | No       | List of activated application for client                                                                                                                                              | Possible values: only `george` at the moment                                |

+ Model

    + Body

            {
                "customerId": "ABC1234567890123",
                "firstname": "Max",
                "lastname": "Mustermann",
                "salutation": "Maxi Mustermanne",
                "alias": "MusterSuperman",
                "instituteId": 188,
                "supportCategory": "PREMIUM",
                "marketingInfoAcceptance": "ACCEPTED",
                "gender": "MALE",
                "dateOfBirth": "1977-04-01",
                "lastlogin": "2013-11-28T16:18:10Z"
            }

### Retrieve a user profile [GET]
Get basic information about the user whom provided authorization token belongs to / represents.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**Profile** type containing basic "netbanking" configuration information about the user.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Profile][]

### Update a user profile [PUT]
This endpoint is used to update profile of user whom provided authorization token belongs to / represents. The resource is a signable resource. To apply the changes to the actual user profile the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Profile** resource with updated attributes, currently only marketingInfoAcceptance can be updated. Changes on not editable fields are ignored (No error message is returned).

#### Reply
**Profile** type containing updated "netbanking" configuration information about the user.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                        |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|---------------------------------------------------------------|
| 1     | profile                 | Profile     | M        | Profile object                                         |                                                               |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                               |


#### Error codes
Error code             | Scope                   | Purpose
---------------------- | ----------------------- | ----------------------------
`INVALID`              | marketingInfoAcceptance | Only `ACCEPTED` and `NOT_ACCEPTED` values are valid.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "customerId": "ABC1234567890123",
                "firstname": "Max",
                "lastname": "Mustermann",
                "individualGreeting": "Hi Muster",
                "marketingInfoAcceptance": "ACCEPTED",
                "gender": "MALE"
            }

+ Response 200 (application/json)

    + Body

            {
                "profile": {
                    "customerId": "ABC1234567890123",
                    "firstname": "Max",
                    "lastname": "Mustermann",
                    "individualGreeting": "Hi Muster",
                    "alias": "MusterSuperman",
                    "instituteId": 188,
                    "supportCategory": "PREMIUM",
                    "marketingInfoAcceptance": "ACCEPTED",
                    "gender": "MALE",
                    "dateOfBirth": "1977-04-01",
                    "lastlogin": "2013-11-28T16:18:10Z"
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016006691"
                }
            }


## ProfileLogininfo [/netbanking/my/profile/logininfo]
Resource **ProfileLogininfo** consist of only *lastlogin* structure, ARRAY of pairs *lastlogin* and *channel* (Only the last login for particular channel/application).

Description of **ProfileLogininfo** attributes: 

| Level | Attribute name          | Type/Enum | Mand/Opt | Attribute description                                                          | Expected values/format                       |
|-------|-------------------------|-----------|----------|--------------------------------------------------------------------------------|----------------------------------------------|
| 1     | lastlogin               | ARRAY of  | O        | Collection of all available last logins of customer via different channels     |                                              |
| 2     | channel                 | TEXT      | M        | Channel/Application identification for particular login                        | Possible values (Local specific values could be defined) : `George`, `GeorgeGo`, `QuickCheck`, `CardControl`, `CallCenter`, `IVR`, `Business24` |
| 2     | lastlogin               | DATETIME  | M        | Date and time of the last login of customer via particular channel/application | ISO dateTime format: YYYY-MM-DDThh:mm:ssZ    |

+ Model

    + Body

            {
                "lastlogin": [
                    {
                        "channel": "George",
                        "lastlogin": "2014-01-23T11:38:52Z"
                    },
                    {
                        "channel": "QuickCheck",
                        "lastlogin": "2014-01-07T15:04:15Z"
                    }
                ]
            }

### Retrieve list of user last logins [GET]
Get list of the last logins of the user whom provided authorization token belongs to / represents.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **ProfileLogininfo** resource containing "lastlogin" structure as array of all available last login dates and the channels of the respective logins.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [ProfileLogininfo][]


# Group Contacts
Contacts-related resources of *Banking Services API*.

## Contact [/netbanking/my/contacts/{id}]
Resource Contact represents one contact information (address, phone, fax or email) of user/client.

Description of **Contact** resource attributes: 

| Level | Attribute name     | Type/Enum   | Mand/Opt | Editable | Attribute description                                                                         | Expected values/format                                                               |
|-------|--------------------|-------------|----------|----------|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 1     | id                 | TEXT        | M        | No       | Internal contact ID                                                                           |                                                                                      |
| 1     | type               | ENUM        | M        | No       | Type of contact (address, phone, fax or email)                                                | ENUM values: [ADDRESS, PHONE, FAX, EMAIL]                                            |
| 1     | flags              | FLAGS       | O        | Yes      | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values - see table below                                                       |
| 1     | address            | structure   | C        | Yes      | Address structure filled only for contact type - address                                      |                                                                                      |
| 2     | type               | ENUM        | M        | Yes      | Type of address                                                                               | ENUM values: [PERMANENT_RESIDENCE, SECONDARY_RESIDENCE, COMPANY_RESIDENCE, UNKNOWN]  |
| 2     | typeI18N           | TEXT        | O        | No       | Localized name of address type (provided by BE)                                               | Domain values (DE/EN): `Hauptwohnsitz`/`Permanent residence`, `Nebenadresse`/`Secondary residence`, `Firmenadresse`/`Company address`, `Unbekannt`/`Unknown`  |
| 2     | description        | TEXT35      | O        | Yes      | More detailed description of addressee (e.g. profession), company name or department          |                                                                                      |
| 2     | street             | TEXT35      | C        | Yes      | Street name with optional abbreviation for street type or location name (for places with unnamed streets) |                                                                          |
| 2     | streetNumber       | TEXT10      | O        | Yes      | Street number with optional abbreviation (orientation number related to the street in CZ and SK)          |                                                                          |
| 2     | buildingApartment  | TEXT35      | O        | Yes      | Building name/number or building descriptive number unique within the town/village/location (in CZ, SK), floor and apartment number with abbreviations |                             |
| 2     | postBox            | TEXT35      | C        | Yes      | Post Box name, number (street, streetNumber, buildingApartment couldn't be provided in this case)         |                                                                          |
| 2     | zipCode            | TEXT10      | M        | Yes      | Postal ZIP Code                                                                               |                                                                                      |
| 2     | city               | TEXT35      | M        | Yes      | City/village name with optional county or city district name/number                           |                                                                                      |
| 2     | country            | ISO3166     | M        | Yes      | ISO 3166 ALPHA-2 code of country (e.g. AT)                                                    |                                                                                      |
| 2     | flags              | FLAGS       | O        | Yes      | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values: `mailingAddress`                                                       |
| 1     | phone              | structure   | C        | Yes      | Phone structure filled only for contact type - phone                                          |                                                                                      |
| 2     | type               | ENUM        | M        | Yes      | Type of phone contact                                                                         | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                             |
| 2     | typeI18N           | TEXT        | O        | No       | Localized name of phone type (provided by BE)                                                 | Domain values (DE/EN): `Privat`/`Private`, `Firma`/`Company`, `Unbekannt`/`Unknown`  |
| 2     | countryCallingCode | TEXT5       | O        | Yes      | Country calling code as international phone number prefix                                     | E.g. "0043" or "+43", "00420" or "+420", "00421" or "+421"                           |
| 2     | areaCallingCode    | TEXT6       | O        | Yes      | Local area calling code as country area/mobile operator prefix                                | E.g. "905" or "0905" or "(0)905", "911" or "0911" or "(0)911", "2" or "02" or "(0)2" |
| 2     | phoneNumber        | TEXT15      | M        | Yes      | Masked Phone number, only the last 3 digits are readible, rest is replaced by a 'x'-character | E.g. "xxxxxx789"                                                                     |
| 2     | reachableFrom      | TEXT5       | O        | Yes      | Optional restriction for contact, user can be reached only from this time                     | Format: HH:mm (e.g. 08:00)                                                           |
| 2     | reachableUntil     | TEXT5       | O        | Yes      | Optional restriction for contact, user can be reached only until this time                    | Format: HH:mm (e.g. 17:00)                                                           |
| 2     | reachableDays      | ARRAY of    | O        | Yes      | List of optional reachable days, user can be reached only in defined days                     |                                                                                      |
| 3     | reachableDaysValue | ENUM        | M        | Yes      | Reachable days - domain key value                                                             | ENUM values: [WEEKDAYS, SATURDAY, SUNDAY_AND_HOLIDAYS]                               |
| 3     | reachableDaysI18N  | TEXT        | M        | Yes      | Reachable days - Localized name of domain key value                                           | Domain values (DE/EN): `Wochentage`/`Weekdays`, `Samstag`/`Saturday`, `Sonn- und Feiertage`/`Sundays and holidays` |
| 2     | flags              | FLAGS       | O        | Yes      | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values: `mobile`                                                               |
| 1     | fax                | structure   | C        | Yes      | Fax structure filled only for contact type - fax                                              |                                                                                      |
| 2     | type               | ENUM        | M        | Yes      | Type of fax contact                                                                           | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                             |
| 2     | typeI18N           | TEXT        | O        | No       | Localized name of fax type (provided by BE)                                                   | Domain values (DE/EN): `Privat`/`Private`, `Firma`/`Company`, `Unbekannt`/`Unknown`  |
| 2     | countryCallingCode | TEXT5       | O        | Yes      | Country calling code as international fax number prefix                                       | E.g. "0043" or "+43", "00420" or "+420", "00421" or "+421"                           |
| 2     | areaCallingCode    | TEXT6       | O        | Yes      | Local area calling code as country area fax number prefix                                     | E.g. "2" or "02" or "(0)2"                                                           |
| 2     | phoneNumber        | TEXT15      | M        | Yes      | Masked Phone number, only the last 3 digits are readible, rest is replaced by a 'x'-character | E.g. "xxxxxx789"                                                                     |
| 1     | email              | structure   | C        | Yes      | Email structure filled only for contact type - email                                          |                                                                                      |
| 2     | type               | ENUM        | M        | Yes      | Type of email contact                                                                         | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                             |
| 2     | typeI18N           | TEXT        | O        | No       | Localized name of email type (provided by BE)                                                 | Domain values (DE/EN): `Privat`/`Private`, `Firma`/`Company`, `Unbekannt`/`Unknown`  |
| 2     | email              | EMAIL50     | M        | Yes      | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                           | E.g. "john.doe@test.com"                                                             |

The following flags can be applied to *flags* attribute for any contact structure:

Flag                | Description
------------------- | -----------------------------------------------
`mainContact`       | Particular contact is marked as main contact. Note: main contact can never be deletable.
`editable`          | Contact is editable by the user
`deletable`         | Contact is deletable by the user

+ Parameters
    + id (TEXT) ... ID of the client contact used as part of URI.

+ Model

    + Body

            {
                "id": "CB4A17822810730C",
                "type": "ADDRESS",
                "flags": [
                    "mainContact",
                    "editable"
                ],
                "address": {
                    "type": "PERMANENT_RESIDENCE",
                    "typeI18N": "Permanent residence",
                    "street": "Landstrasse 55",
                    "zipCode": "1050",
                    "city": "Wien",
                    "country": "AT",
                    "flags": [
                        "mailingAddress"
                    ]
                }
            }

### Retrieve a single user contact [GET]
Get one single contact information (type of address, phone, fax or email) identified by ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**Contact** resource with one contact information of the client.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Contact][]

### Update a single user contact [PUT]
Change one single contact (address or phone or fax or email) identified by ID with `editable` flag. The resource is a signable resource. To apply the changes to the user contact the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Contact** resource with updated attributes, only ID and type can't be updated.

#### Reply
**Contact** resource containing updated information of the user contact.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt  | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|-----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | contact                 | Contact     | M         | Contact object                                         |                                                                |
| 1     | signInfo                | SIGNINFO    | M         | SignInfo Details                                       |                                                                |

#### Error codes
Error code                      | Scope    | Purpose
--------------------------------|----------|-----------------------------
`ID_NOT_FOUND`                  | id       | The provided ID does not exist.
`ID_MISMATCH`                   | id       | The ID given in the payload does not match the ID given in the URI.
`CONTACT_NOT_EDITABLE`          | id       | Contact entry can not be updated.
`ONE_CONTACT_ALLOWED`           | id       | Only one contact of corresponding type is allowed.
`TYPE_MISMATCH`                 | type     | Type and contact object do not match.
`FIELD_MISSING`                 | type     | Contact object of corresponding type is missing.
`VALUE_INVALID`                 | street   | Either street or postbox must be entered for address type.
`VALUE_INVALID`                 | postBox  | Either street or postbox must be entered for address type.
`ZIPCODE_DOES_NOT_MATCH_CITY`   | zipCode  | Zip code and city must match.
`FIELD_INVALID`                 |          | Possible local validations (format, length) for particular contact attributes 

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "id": "CB4A17822810730K",
                "type": "EMAIL",
                "email": {
                    "type": "PRIVATE",
                    "typeI18N": "Private",
                    "email": "my_new_email@gmail.com"
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "contact": {
                    "id": "CB4A17822810730K",
                    "type": "EMAIL",
                    "flags": [
                        "editable"
                    ],
                    "email": {
                        "type": "PRIVATE",
                        "typeI18N": "Private",
                        "email": "my_new_email@gmail.com"
                    }
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "04397170179000001600999"
                }
            }

### Delete single user contact [DELETE]
Deletes one specific user contact identified by ID with `deletable` flag. The resource is a signable resource. To delete the user contact the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of DELETE resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id             | The provided ID does not exist.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    + Body

            {
                "signInfo": {
                    "state": "OPEN",
                    "signId": "0439717017900000160012334"
                }
            }


## ContactList_and_ContactEntry [/netbanking/my/contacts]
Resource **ContactList** represents collection of client contacts stored in BE. This resource consists of array of *embedded* **Contact** resource items.

Description of **ContactList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                      | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------------|--------------------------|
| 1     | contacts       | ARRAY of CONTACT | O        | Array of contacts of the user (could be empty) (embedded CONTACT resource) |                          |


**ContactEntry** resource represents object of new single Contact entered by the user. Resource uses subset of attributes of embedded **Contact** resource.

Description of **ContactEntry** resource attributes: 

| Level | Attribute name     | Type/Enum   | Mand/Opt | Attribute description                                                                         | Expected values/format                                                               |
|-------|--------------------|-------------|----------|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 1     | type               | ENUM        | M        | Type of contact (address, phone, fax or email)                                                | ENUM values: [ADDRESS, PHONE, FAX, EMAIL]                                            |
| 1     | flags              | FLAGS       | O        | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values - see table below                                                       |
| 1     | address            | structure   | C        | Address structure filled only for contact type - address                                      |                                                                                      |
| 2     | type               | ENUM        | M        | Type of address                                                                               | ENUM values: [PERMANENT_RESIDENCE, SECONDARY_RESIDENCE, COMPANY_RESIDENCE, UNKNOWN]  |
| 2     | description        | TEXT35      | O        | More detailed description of addressee (e.g. profession), company name or department          |                                                                                      |
| 2     | street             | TEXT35      | C        | Street name with optional abbreviation for street type or location name (for places with unnamed streets) |                                                                          |
| 2     | streetNumber       | TEXT10      | O        | Street number with optional abbreviation (orientation number related to the street in CZ and SK)          |                                                                          |
| 2     | buildingApartment  | TEXT35      | O        | Building name/number or building descriptive number unique within the town/village/location (in CZ, SK), floor and apartment number with abbreviations |                             |
| 2     | postBox            | TEXT35      | C        | Post Box name, number (street, streetNumber, buildingApartment couldn't be provided in this case)         |                                                                          |
| 2     | zipCode            | TEXT10      | M        | Postal ZIP Code                                                                               |                                                                                      |
| 2     | city               | TEXT35      | M        | City/village name with optional county or city district name/number                           |                                                                                      |
| 2     | country            | ISO3166     | M        | ISO 3166 ALPHA-2 code of country (e.g. AT)                                                    |                                                                                      |
| 2     | flags              | FLAGS       | O        | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values: `mailingAddress`                                                       |
| 1     | phone              | structure   | C        | Phone structure filled only for contact type - phone                                          |                                                                                      |
| 2     | type               | ENUM        | M        | Type of phone contact                                                                         | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                             |
| 2     | countryCallingCode | TEXT5       | O        | Country calling code as international phone number prefix                                     | E.g. "0043" or "+43", "00420" or "+420", "00421" or "+421"                           |
| 2     | areaCallingCode    | TEXT6       | O        | Local area calling code as country area/mobile operator prefix                                | E.g. "905" or "0905" or "(0)905", "911" or "0911" or "(0)911", "2" or "02" or "(0)2" |
| 2     | phoneNumber        | TEXT15      | M        | Masked Phone number, only the last 3 digits are readible, rest is replaced by a 'x'-character | E.g. "xxxxxx789"                                                                     |
| 2     | reachableFrom      | TEXT5       | O        | Optional restriction for contact, user can be reached only from this time                     | Format: HH:mm (e.g. 08:00)                                                           |
| 2     | reachableUntil     | TEXT5       | O        | Optional restriction for contact, user can be reached only until this time                    | Format: HH:mm (e.g. 17:00)                                                           |
| 2     | reachableDays      | ARRAY of    | O        | List of optional reachable days, user can be reached only in defined days                     |                                                                                      |
| 3     | reachableDaysValue | ENUM        | M        | Reachable days - domain key value                                                             | ENUM values: [WEEKDAYS, SATURDAY, SUNDAY_AND_HOLIDAYS]                               |
| 3     | reachableDaysI18N  | TEXT        | M        | Reachable days - Localized name of domain key value                                           | Domain values (DE/EN): `Wochentage`/`Weekdays`, `Samstag`/`Saturday`, `Sonn- und Feiertage`/`Sundays and holidays` |
| 2     | flags              | FLAGS       | O        | Array of optional flag values, the absence of a certain string is considered as “false”       | Flags values: `mobile`                                                               |
| 1     | fax                | structure   | C        | Fax structure filled only for contact type - fax                                              |                                                                                      |
| 2     | type               | ENUM        | M        | Type of fax contact                                                                           | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                         |
| 2     | countryCallingCode | TEXT5       | O        | Country calling code as international fax number prefix                                       | E.g. "0043" or "+43", "00420" or "+420", "00421" or "+421"                           |
| 2     | areaCallingCode    | TEXT6       | O        | Local area calling code as country area fax number prefix                                     | E.g. "2" or "02" or "(0)2"                                                           |
| 2     | phoneNumber        | TEXT15      | M        | Masked Phone number, only the last 3 digits are readable, rest is replaced by a 'x'-character | E.g. "xxxxxx789"                                                                     |
| 1     | email              | structure   | C        | Email structure filled only for contact type - email                                          |                                                                                      |
| 2     | type               | ENUM        | M        | Type of email contact                                                                         | ENUM values: [PRIVATE, COMPANY, UNKNOWN]                                         |
| 2     | email              | EMAIL50     | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                           | E.g. "john.doe@test.com"                                                             |

The following flags can be applied to *flags* attribute for any contact structure:

Flag                | Description
------------------- | -----------------------------------------------
`mainContact`       | Particular contact is marked as main contact.

### Retrieve list of all client contacts [GET]
Get list of all client's contacts of all types.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **ContactList** without paging and possibly empty (omitted) array of **Contact** of client.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    + Body

            {
                "contacts": [
                    {
                        "id": "CB4A17822810730C",
                        "type": "ADDRESS",
                        "flags": [
                            "mainContact",
                            "editable"
                        ],
                        "address": {
                            "type": "PERMANENT_RESIDENCE",
                            "typeI18N": "Permanent residence",
                            "street": "Landstrasse 55",
                            "zipCode": "1050",
                            "city": "Wien",
                            "country": "AT",
                            "flags": [
                                "mailingAddress"
                            ]
                        }
                    },
                    {
                        "id": "CB4A178228107311",
                        "type": "ADDRESS",
                        "flags": [
                            "editable",
                            "deletable"
                        ],
                        "address": {
                            "type": "COMPANY_RESIDENCE",
                            "typeI18N": "Company address",
                            "postBox": "P.O. Box 562",
                            "zipCode": "1050",
                            "city": "Wien",
                            "country": "AT",
                            "flags": [
                                "mailingAddress"
                            ]
                        }
                    },
                    {
                        "id": "CB4A178228107319",
                        "type": "ADDRESS",
                        "flags": [
                            "editable",
                            "deletable"
                        ],
                        "address": {
                            "type": "SECONDARY_RESIDENCE",
                            "typeI18N": "Secondary residence",
                            "street": "Tomasikova 55",
                            "zipCode": "82105",
                            "city": "Bratislava",
                            "country": "SK"
                        }
                    },
                    {
                        "id": "CB4A178228107355",
                        "type": "PHONE",
                        "flags": [
                            "mainContact",
                            "editable"
                        ],
                        "phone": {
                            "type": "PRIVATE",
                            "typeI18N": "Private",
                            "countryCallingCode": "0043",
                            "areaCallingCode": "911",
                            "phoneNumber": "xxxxxx789",
                            "reachableFrom": "09:00",
                            "reachableUntil": "20:00",
                            "reachableDays": [
                                {
                                    "reachableDaysValue": "WEEKDAYS", 
                                    "reachableDaysI18N": "Weekdays"
                                },
                                {
                                    "reachableDaysValue": "SATURDAY", 
                                    "reachableDaysI18N": "Saturday"
                                }
                            ],
                            "flags": [
                                "mobile"
                            ]
                        },
                    },
                    {
                        "id": "CB4A178228107359",
                        "type": "PHONE",
                        "flags": [
                            "editable"
                        ],
                        "phone": {
                            "type": "PRIVATE",
                            "typeI18N": "Private",
                            "areaCallingCode": "911",
                            "phoneNumber": "xxxxxx666",
                            "flags": [
                                "mobile"
                            ]
                        }
                    },
                    {
                        "id": "CB4A178228107999",
                        "type": "EMAIL",
                        "flags": [
                            "editable",
                            "deletable"
                        ],
                        "email": {
                            "type": "PRIVATE",
                            "typeI18N": "Private",
                            "email": "jan.muster@test.com"
                        }
                    }
                ]
            }

### Create a single user contact [POST]
Create one single contact (address or phone or fax or email) entered by user. The resource is a signable resource. To create new user contact the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**ContactEntry** resource with all relevant attributes to create contact.

#### Reply
**Contact** resource containing information about new user contact.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of POST resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | contact                 | Contact     | M        | Contact object                                         |                                                                |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code                          | Scope    | Purpose
------------------------------------|----------|-----------------------------
`ONE_CONTACT_ALLOWED`               | id       | Only one contact of corresponding type is allowed.
`TYPE_MISMATCH`                     | type     | Type and contact object do not match.
`FIELD_MISSING`                     | type     | Contact object of corresponding type is missing.
`VALUE_INVALID`                     | street   | Either street or postbox must be entered for address type.
`VALUE_INVALID`                     | postBox  | Either street or postbox must be entered for address type.
`ZIPCODE_DOES_NOT_MATCH_CITY`       | zipCode  | Zip code and city must match.
`MAX_NUMBER_OF_ADDRESSES_EXCEEDED`  | address  | The maximum number of addresses supported by the backend is exceeded. (Could be local specific)
`FIELD_INVALID`                     |          | Possible local validations (format, length) for particular contact attributes 

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "type": "ADDRESS",
                "address": {
                    "type": "PERMANENT_RESIDENCE",
                    "street": "Panská",
                    "streetNumber": "12",
                    "buildingApartment": "391",
                    "zipCode": "60200",
                    "city": "Brno",
                    "country": "CZ",
                    "flags": [
                        "mailingAddress"
                    ]
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "contact": {
                    "id": "CB4A17822810730M",
                    "type": "ADDRESS",
                    "flags": [
                        "editable",
                        "deletable"
                    ],
                    "address": {
                        "type": "PERMANENT_RESIDENCE",
                        "street": "Panská",
                        "streetNumber": "12",
                        "buildingApartment": "391",
                        "zipCode": "60200",
                        "city": "Brno",
                        "country": "CZ",
                        "flags": [
                            "mailingAddress"
                        ]
                    }
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "0439717017900000160012337"
                }
            }


# Group Settings
Settings-related resources of *Banking Services API*.

## Settings [/netbanking/my/settings]
Resource Settings represents serverside settings of user/client.

Description of **Settings** resource attributes: 

| Level | Attribute name         | Type/Enum     | Mand/Opt | Editable  | Attribute description                                                                   | Expected values/format                        |
|-------|------------------------|---------------|----------|-----------|-----------------------------------------------------------------------------------------|-----------------------------------------------|
| 1     | language               | ENUM          | O        | No        | User preferred language stored in BackEnd, default is local language. ISO 639-1 values. | ENUM values: [en, de, cs, sk, hr, sr, ro, hu] |
| 1     | tacPhoneNumbers        | ARRAY of TEXT | O        | No        | List of available phone numbers, where to send a TAC SMS code                           | Phone numbers are masked                      |
| 1     | authorizationType      | ENUM          | O        | No        | User preferred authorization method type                                                | ENUM values: [TAC,TAN,SMS,PKI,GRID_CARD,EOK,VOICE,DISPLAY_CARD,M_TOKEN] other local authorization type has to be defined |
| 1     | flags                  | FLAGS         | O        | Yes       | Array of optional flag values, the absence of a certain string is considered as “false” | Flags values - see table below                |

**Note:** Current CSAS User story doesn't use *language* field provided by this resource from BackEnd. When user selects his preferred language (either in George – menu Settings, or on login page), this language is saved in **cookies**. When George/login page is loaded, it selects display language based on cookies or browser header

The following flags can be applied to field *flags* in **Settings** resource:

Flag                            | Description
------------------------------- | -----------------------------------------------
`displayInsurances`             | Insurance contracts are displayed in online banking (George).
`displayLeasings`               | Leasing contracts are displayed in online banking (George).
`displayBuildings`              | Buildings society contracts are displayed in online banking (George).
`displayCreditCards`            | Credit cards are displayed in online banking (George).
`displayInvestments`            | Investment securities are displayed in online banking (George).

+ Model

    + Body

            {
                "language": "en",                     
                "tacPhoneNumbers": [                  
                    "+420*****1234",
                    "+421*****1007"
                ],
                "authorizationType": "TAC",   
                "flags": [
                    "displayInsurances",
                    "displayLeasings",
                    "displayCreditCards"
                ] 
            }

### Retrieve a user settings [GET]
Get netbanking settings of the user whom provided authorization token belongs to / represents.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Settings** resource containing basic serverside settings of user/client.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Settings][]

### Update a user settings [PUT]
Change the product group display settings (meaning, the flags of the settings object). Flags can only be removed, not added. The resource is a signable resource. To apply the changes to the actual user settings the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Settings** resource with updated attributes, currently only flags can be updated. Changes on not editable fields are ignored (No error message is returned).

#### Reply
**Settings** resource containing updated flags settings information of the user.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | settings                | Settings    | M        | Settings object                                        |                                                                |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code                      | Scope  | Purpose
------------------------------- | ------ | ----------------------------
`PERMISSIONS_CANT_BE_INCREASED` | flags  | A display flag was added instead of removal.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "authorizationType": "TAC",   
                "flags": [
                    "displayInsurances",
                    "displayCreditCards"
                ] 
            }

+ Response 200 (application/json)

    + Body

            {
                "settings": {
                    "language": "en",                     
                    "tacPhoneNumbers": [                  
                        "+420*****1234",
                        "+421*****1007"
                    ],
                    "authorizationType": "TAC",   
                    "flags": [
                        "displayInsurances",
                        "displayCreditCards"
                    ] 
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016006666"
                }
            }


# Group Messages
Message-related resources of *Banking Services API*.

## Message [/netbanking/my/messages/{id}]
Message type represents message in the personal inbox of the user.

Description of **Message** resource/type attributes: 

| Level | Attribute name | Type/Enum | Mand/Opt | Attribute description                                                                   | Expected values/format   |
|-------|----------------|-----------|----------|-----------------------------------------------------------------------------------------|--------------------------|
| 1     | id             | TEXT      | M        | Internal ID of message                                                                  |                          |
| 1     | from           | TEXT      | M        | Source system of message, who created message                                           | e.g. `netbanking`        |
| 1     | subject        | TEXT      | M        | Subject of the message provided by source system (in particular language)               |                          |
| 1     | date           | DATETIME  | M        | Date and time of message creation/generation                                            | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ |
| 1     | body           | HTML      | M        | Text of the message, could be as html                                                   |                          |
| 1     | attachments    | ARRAY of  | O        | Optional array of attachment metadata                                                   |                          |
| 2     | id             | TEXT      | M        | Message attachment ID                                                                   | e.g. 141951229267        |
| 2     | fileName       | TEXT      | M        | File name of Message attachment                                                         | name.extension, like "test.doc" |
| 1     | flags          | FLAGS     | O        | Array of optional flag values, the absence of a certain string is considered as “false” | Flags values - see table below |

The following flags can be applied to field *flags* in **Message** resource:

Flag        | Description
------------|-----------------------------------------------
`mandatory` | This is a mandatory message.
`unread`    | This message is unread by user.
`unsigned`  | This message is unsigned yet.
`warning`   | Type of message is warning.
`info`      | Type of message is information.

+ Parameters
    + id (TEXT) ... ID of the user message in inbox used as part of URI.

+ Model

    + Body

            {
                "id": "131371121204",
                "from": "netbanking",
                "subject": "once again for the API",
                "date": "2013-05-17T00:00:00+02:00",
                "body": "please read this message, really urgent",
                "attachments": [
                    {
                        "id": "141951229267",
                        "fileName": "test.doc"
                    },
                    {
                        "id": "141951230156",
                        "fileName": "test2.pdf"
                    }
                ],
                "flags": [
                    "unread"
                ]
            }

### Retrieve single message from personal inbox [GET]
Get the details of one specific message, read or unread.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Message** resource containing details of one message from user inbox identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Message][]

### Update single message in personal inbox [PUT]
Change one single message identified by ID. This endpoint currently only supports marking a message as (un)read.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

JSON payload consists of only one boolean attribute *read*.

#### Reply
No data is returned on successful change.

Description of PUT resource attributes:
No payload.

#### Error codes
Error code                      | Scope    | Purpose
--------------------------------|----------|-----------------------------
`ID_NOT_FOUND`                  | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "read": true
            }

+ Response 204 (application/json)

### Delete single message from personal inbox [DELETE]
Endpoint removes one specific message identified by ID from user personal inbox.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
No data is returned on successful change.

Description of DELETE resource attributes:
No payload.

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id             | The provided ID does not exist.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 204 (application/json)


## MessageAttachment [/netbanking/my/messages/{id}/attachments/{aId}]
**MessageAttachment** resource represents one single attachment of message in the personal inbox of the user.
This resource has no JSON payload since it is only binary (BASE64FILE) representation of attachment. 

+ Parameters
    + id (TEXT) ... ID of the user message in inbox used as part of URI.
    + aId (TEXT) ... ID of the particular attachment from identified message in inbox used as part of URI.
    
+ Model

    + Body

            {
            }

### Retrieve single attachment of message from personal inbox [POST]
Get the binary representation of attachment (identified by attachment ID) of one specific message (identified by message ID) from user inbox.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **MessageAttachment** resource containing the binary representation of an attachment, with an additional “Content-Disposition” header in order to instruct the browser to open a save dialog.

Description of POST resource attributes:
Attachment.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id, aId  | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/octet-stream)

    + Headers

            Content-Disposition: attachment; filename=filename.ext


## MessageList [/netbanking/my/messages{?size,page,sort,order}]
Resource Message List represents collection of messages in the personal inbox of the user.
This resource consists of paging attributes and array of *embedded* **Message** type items.

Description of **MessageList** resource/type attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page  (provided only when exist)            |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | messages       | ARRAY of MESSAGE | O        | Array of messages in the personal inbox of the user (could be empty) (embedded MESSAGE type/resource) |  |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are: `from`, `date` and `id`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 20,
                "messages": [
                    {
                        "id": "131371121204",
                        "from": "netbanking",
                        "subject": "once again for API testing",
                        "date": "2013-05-17T00:00:00+02:00",
                        "body": "please read this message, really urgent",
                        "attachments": [
                            {
                                "id": "141951229267",
                                "fileName": "test.doc"
                            },
                            {
                                "id": "141951230156",
                                "fileName": "test2.doc"
                            }
                        ],
                        "flags": [
                            "unread"
                        ]
                    },
                    {
                        "id": "131370923151",
                        "from": "Absender",
                        "subject": "nb MUSS Nachricht",
                        "date": "2013-05-17T00:00:00+02:00",
                        "body": "Inhalt",
                        "flags": [
                            "mandatory"
                        ]
                    },
                    {
                        "id": "131370922317",
                        "from": "Absender",
                        "subject": "nb KANN Nachricht",
                        "date": "2013-05-17T00:00:00+02:00",
                        "body": "Inhalt"
                    }
                ]
            }

### Retrieve list of messages from personal inbox [GET]
Get list of all messages, read or unread, mandatory and non-mandatory. This call is paginated and can be sorted by fields `from`, `date` and `id`. This call might return different messages based on appId of the caller (for example, some messages might be specific to an particular application). Which messages can be seen by which application should be configured on the local WebAPI server side.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **MessageList** with paging info and possibly empty (omitted) array of **Message** items from user inbox.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [MessageList][]


## MandatoryMessageList [/netbanking/my/messages/mandatory]
Resource Mandatory Message List represents collection of messages with flag `mandatory` in the personal inbox of the user.
This resource consists of array of *embedded* **Message** type items (without paging attributes).

Description of **MandatoryMessageList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | messages       | ARRAY of MESSAGE | O        | Array of mandatory messages in the personal inbox of the user (could be empty) (embedded MESSAGE resource) |  |


+ Model

    + Body

            {
                "messages": [
                   {
                        "id": "131371121204",
                        "from": "netbanking",
                        "subject": "once again for the API",
                        "date": "2013-05-17T00:00:00+02:00",
                        "body": "please read this message, really urgent",
                        "attachments": [
                            {
                                "id": "141951229267",
                                "fileName": "test.doc"
                            },
                            {
                                "id": "141951230156",
                                "fileName": "test2.pdf"
                            }
                        ],
                        "flags": [
                            "mandatory"
                        ]
                    },
                    {
                        "id": "131370923151",
                        "from": "Absender",
                        "subject": "nb MUSS Nachricht",
                        "date": "2013-05-17T00:00:00+02:00",
                        "body": "Must see offer",
                        "flags": [
                            "mandatory"
                        ]
                    }
                ]
            }

### Retrieve all mandatory messages from personal inbox [GET]
Return all mandatory messages. This call can return different messages based on appId of the caller (for example, some messages could be specific to an application). Which messages can be seen by which application can be configured on the local WebAPI server side.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **MandatoryMessageList** with possibly empty (omitted) array of **Message** items with mandatory flag.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [MandatoryMessageList][]


# Group Limits
Limit-related resources of *Banking Services API*.

## AuthorizationLimits [/netbanking/my/authorizationLimits/{id}]
Authorization Limits type represents payment order entry limits defined for particular authorization method identified by ID.

**Note:** Local specification of this resource depends on authorization methods used in particular country.

Description of **AuthorizationLimits** resource attributes: 

| Level | Attribute name    | Type/Enum | Mand/Opt | Editable | Attribute description                                                                | Expected values/format            |
|-------|-------------------|-----------|----------|----------|--------------------------------------------------------------------------------------|-----------------------------------|
| 1     | id                | TEXT      | M        | No       | Internal ID for limit definition for authorization type, channel, application        | If internal ID doesn't exist, ID could be generated using authorizationType, channelId and applicationId values |
| 1     | authorizationType | ENUM      | M        | No       | Authorization method type for which is limit defined                                 | ENUM values: [TAC, TAN, SMS, PKI, GRID_CARD, EOK, VOICE, DISPLAY_CARD, M_TOKEN] other local authorization type has to be defined |
| 1     | channelId         | ENUM      | M        | No       | ID of the channel for which is limit defined                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, THIRD_PARTY, UNKNOWN] - limit valid for all channels, not particularly defined |
| 1     | applicationId     | ENUM      | M        | No       | ID of the application for which is limit defined                                     | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, CARD_CONTROL, TRANSACTIONS, BUSINESS24, UNKNOWN], future new apps from third parties providers, `UNKNOWN` - limit valid for all applications, not particularly defined |
| 1     | dailyLimit        | AMOUNT    | O        | Yes      | Daily limit for particular authorization method (_embedded AMOUNT type)              | Fields value, precision, currency |
| 1     | transactionLimit  | AMOUNT    | O        | Yes      | Transaction limit for particular authorization method (_embedded AMOUNT type)        | Fields value, precision, currency |
| 1     | maxBankLimit      | AMOUNT    | O        | No       | Maximal daily limit for authorization method defined by bank (_embedded AMOUNT type) | Fields value, precision, currency |

+ Parameters
    + id (TEXT) ... ID of the authorization limits valid for particular user, authorization method and channel/application used as part of URI.

+ Model

    + Body

            {
                "id": "934872973982",
                "authorizationType": "TAC",
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE",
                "dailyLimit": {
                    "value": 400000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "transactionLimit": {
                    "value": 100000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "maxBankLimit": {
                    "value": 1700000,
                    "precision": 2,
                    "currency": "EUR"
                }
            }

### Get user payment order authorization limits [GET]
Return local specific payment order entry limits valid for combination of user, authorization method and used channel/application. For example user could define different limits for TAN authorization via George and mobile applications.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **AuthorizationLimits** with limit values defined in *embedded* type **Amount** for particular authorization limit ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AuthorizationLimits][]

### Update a user payment order authorization limits [PUT]
Change the user payment order authorization limits valid for combination of user, authorization method and used channel/application (Identified by ID). The resource is a signable resource. Requested limits are not immediately applied but stored as signing-request. The response returns a signInfo type which should be used to sign the changes in signing-workflow.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**AuthorizationLimits** resource with updated limit amounts. 

#### Reply
**AuthorizationLimits** resource containing updated payment order limits information for the user, authorization method and channel/application.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum              | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|------------------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | limits                  | AuthorizationLimits    | M        | AuthorizationLimits object                             |                                                                |
| 1     | signInfo                | SIGNINFO               | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code           | Scope            | Purpose
-------------------- | ---------------- | ----------------------------
`ID_NOT_FOUND`       | id               | The provided ID does not exist.
`ID_MISMATCH`        | id               | The ID given in the payload does not match the ID given in the URI.
`LIMIT_EXCEEDED`     | dailyLimit       | Max amount (defined by bank) exceeded for this limit.
`LIMIT_EXCEEDED`     | transactionLimit | Max amount (defined by bank) exceeded for this limit.
`FIELD_MISSING`      | dailyLimit       | Mandatory limit for particular authorization method is missing.
`FIELD_MISSING`      | transactionLimit | Mandatory limit for particular authorization method is missing.
`INVALID`            | dailyLimit       | Limit amount value is invalid (f.i. negative, wrong precision)
`INVALID`            | transactionLimit | Limit amount value is invalid (f.i. negative, wrong precision)

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "id": "934872973982",
                "authorizationType": "TAC",
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE",
                "dailyLimit": {
                    "value": 270000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "transactionLimit": {
                    "value": 70000,
                    "precision": 2,
                    "currency": "EUR"
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "limits": {
                    "id": "934872973982",
                    "authorizationType": "TAC",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "dailyLimit": {
                        "value": 270000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transactionLimit": {
                        "value": 70000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016006666"
                }
            }


## AuthorizationLimitsList [/netbanking/my/authorizationLimits{?channel}]
Authorization Limits List type represents all payment order entry limits defined for combination of user, authorization methods and channels/applications.
This resource consists of array of *embedded* **AuthorizationLimits** type items.

**Note:** Local specification of this resource depends on authorization methods used in particular country.


Description of **AuthorizationLimitsList** resource/type attributes: 

| Level | Attribute name | Type/Enum                     | Mand/Opt | Attribute description                                                                                                                    | Expected values/format   |
|-------|----------------|-------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| 1     | limits         | ARRAY of AuthorizationLimits  | M        | Array of authorization limits defined for user, authorization methods and channels/applications. (embedded AuthorizationLimits resource) |                          |

+ Parameters
    + channel (TEXT, optional) ... This call delivers all user authorization limits defined for all channels/applications (no channel parameter provided) or limits for specific channel/application (by default `George` should be used for George application). Channel URI parameter possible values: `George`, `GeorgeGo`, `Business24`. *(Functionality is currently not provided in George AT)*

+ Model

    + Body

            {
                "limits": [
                    {
                        "id": "934872973982",
                        "authorizationType": "TAC",
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "dailyLimit": {
                            "value": 400000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "transactionLimit": {
                            "value": 100000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "maxBankLimit": {
                            "value": 1700000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "id": "934872973987",
                        "authorizationType": "TAC",
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE_GO",
                        "dailyLimit": {
                            "value": 150000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "transactionLimit": {
                            "value": 90000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "maxBankLimit": {
                            "value": 900000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "id": "934872973988",
                        "authorizationType": "TAN",
                        "channelId": "NET_BANKING",
                        "applicationId": "UNKNOWN",
                        "dailyLimit": {
                            "value": 299999,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "transactionLimit": {
                            "value": 100000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "maxBankLimit": {
                            "value": 500000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    }
                ]
            }

### Get user payment order authorization limits [GET]
Return all user local specific payment order entry limits for for all user active authorization methods and channels/applications used in country.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **AuthorizationLimitsList** with array of **AuthorizationLimits** items for user.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AuthorizationLimitsList][]


## PaymentRemainingLimits [/netbanking/my/orders/payments/limits]
Payment Remaining Limits resource represents collection of remaining daily limit amounts for all user authorization methods and channels/applications used for signing new payment order in particular day.

**Note:** Local specification of this resource depends on authorization methods used in particular country.

Description of **PaymentRemainingLimits** resource attributes: 

| Level | Attribute name    | Type/Enum | Mand/Opt | Attribute description                                                                                                     | Expected values/format            |
|-------|-------------------|-----------|----------|---------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| 1     | remainingLimits   | ARRAY of  | O        | Collection of remaining daily limit amounts for all user authorization methods and channels/applications                  |                                   |
| 2     | authorizationType | ENUM      | M        | Authorization method type for which is limit defined                                                                      | ENUM values: [TAC,TAN,SMS,PKI,GRID_CARD,EOK,VOICE,DISPLAY_CARD,M_TOKEN]  other local authorization type has to be defined |
| 2     | channelId         | ENUM      | M        | ID of the channel for which is limit defined                                                                              | ENUM values: [NET_BANKING,MOBILE_BANKING,HOME_BANKING,THIRD_PARTY,UNKNOWN] - remaining limit amount valid for all channels, not particularly defined |
| 2     | applicationId     | ENUM      | M        | ID of the application for which is limit defined                                                                          | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, CARD_CONTROL, TRANSACTIONS, BUSINESS24, UNKNOWN] - remaining limit amount valid for all applications, not particularly defined |
| 2     | remainingAmount   | AMOUNT    | M        | Remaining Daily amount which can be transferred using particular authorization method and channel (_embedded AMOUNT type) | Fields value, precision, currency |

+ Model

    + Body

            {
                "remainingLimits": [
                    {
                        "authorizationType": "TAC",
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "remainingAmount": {
                            "value": 99900,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "authorizationType": "TAC",
                        "channelId": "MOBILE_BANKING",
                        "applicationId": "GEORGE_GO",
                        "remainingAmount": {
                            "value": 25000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "authorizationType": "TAN",
                        "channelId": "NET_BANKING",
                        "applicationId": "UNKNOWN",
                        "remainingAmount": {
                            "value": 900000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    }
                ]
            }

### Get remaining amounts for payment orders authorizations [GET]
Return remaining amounts of user local specific daily limits for authorization methods used in country. Unsigned orders will not be considered in the calculation of the remaining amounts.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **PaymentRemainingLimits** with limit remaining amounts.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [PaymentRemainingLimits][]


# Group Accounts
Account-related resources of *Banking Services API*.

## Account [/netbanking/my/accounts/{id}]
Account type represents user account product of different account types (current account, saving account, loan/mortgage account).

Description of **Account** resource/type attributes: 

| Level | Attribute name          | Type/Enum | Mand/Opt | Editable | Attribute description                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------------|-----------|----------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | id                      | TEXT      | M        | No       | Internal ID as reference for account                                                                                                                               |                                                                                                                                                                                    |
| 1     | accountno               | ACCOUNTNO | M        | No       | Account number (_embedded ACCOUNTNO type)                                                                                                                          |                                                                                                                                                                                    |
| 1     | type                    | ENUM      | M        | No       | Product Type of account (Current, Saving, Loan).                                                                                                                   | ENUM values: [CURRENT, SAVING, LOAN]                                                                                                                                               |
| 1     | subtype                 | TEXT      | O        | No       | Product Subtype of account, categorization of products below particular type (this local specific value could be used to define FE behavior of product group).     | Possible values, for type=CURRENT: `CURRENT_ACCOUNT`, `GIRO_ACCOUNT`, `FOREIGN_ACCOUNT`; for type=SAVING: `SAVING_ACCOUNT`, `TERM_DEPOSIT`, `SAVING_PLUS`; for type=LOAN: `LOAN_ACCOUNT`, `MORTGAGE` |
| 1     | product                 | TEXT      | M        | No       | Product name of account. Localization of expected values and George FE behavior is needed, because Erste Group doesn't use harmonized group product definitions.   | Values in AT: `girokonto`, `bonuscard`, `einlagekonto`, `kapitalsparkonto`, `praemiensparkonto`, `profitcard`, `splussparen`, `terminsparkonto`, `wachstumssparen`, `finanzierung` |
| 1     | productI18N             | TEXT      | M        | No       | Localized product name of account depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language).    |                                                                                                                                                                                    |
| 1     | description             | TEXT      | O        | No       | Account description, used to provide account owner full name                                                                                                       |                                                                                                                                                                                    |
| 1     | alias                   | TEXT60    | O        | Yes      | Account alias stored in BackEnd (could be reused in mobile app as well).                                                                                           |                                                                                                                                                                                    |
| 1     | balance                 | AMOUNT    | M        | No       | Account balance for Current, Saved amount for Saving, Principal Outstanding for Loan/Mortgage. Balance is provided only if account is not offline/in closing       | Fields value, precision, currency                                                                                                                                                  |
| 1     | disposable              | AMOUNT    | O        | No       | Disposable balance for Current account is provided only if account is not offline/in closing (_embedded AMOUNT type)                                               | Fields value, precision, currency                                                                                                                                                  |
| 1     | overdraft               | AMOUNT    | O        | No       | Overdraft amount for Current account (_embedded AMOUNT type)                                                                                                       | Fields value, precision, currency                                                                                                                                                  |
| 1     | overdraftDueDate        | DATE      | O        | No       | Overdraft due date, when there is not automated prolongation of overdraft                                                                                          | ISO Date format                                                                                                                                                                    |
| 1     | creditInterestRate      | FLOAT     | O        | No       | Basic credit Interest rate, used for Current and Saving account                                                                                                    | Value in percentage, e.g. 0,5 will be displayed as 0,5 %                                                                                                                           |
| 1     | creditInterestRateBonus | FLOAT     | O        | No       | Bonus credit Interest rate, used for Current and Saving account, which local bank will add to Basic credit rate under some conditions for related product usage    | Value in percentage, e.g. 0,5 will be displayed as 0,5 %, then total credit rate could be 0,5% + 0,5% = 1,0%                                                                       |
| 1     | debitInterestRate       | FLOAT     | O        | No       | Basic debit Interest rate, used for Ovedraft, Loan and Mortgage account                                                                                            | Value in percentage, e.g. 11,5 will be displayed as 11,5 %                                                                                                                         |
| 1     | debitInterestRateBonus  | FLOAT     | O        | No       | Bonus debit Interest rate, used for Loan and Mortgage account, which local bank will deduct from Basic debit rate under some conditions for related product usage  | Value in percentage, e.g. -2,0 will be displayed as -2,0 %, then total debit rate could be 11,5% - 2,0% = 9,5%                                                                     |
| 1     | penaltyInterestRate     | FLOAT     | O        | No       | Penalty debit Interest rate, used for not allowed Ovedraft, rate after due date for Loan                                                                           | Value in percentage, e.g. 19,5 will be displayed as 19,5 %                                                                                                                         |
| 1     | saving                  | structure | O        | No       | Structure for Saving accounts                                                                                                                                      |                                                                                                                                                                                    |
| 2     | savingGoal              | TEXT35    | O        | No       | Saving goal - saving purpose defined by user or selected from predefined values                                                                                    | User text or possible predefined values: `ELECTRONICS`,`WHITE_GOODS`,`HOLIDAYS`,`SPORT_EQUIPMENT`,`FURNITURE`,`CARS_AND_ACCESSORIES`,`HOBBIES_AND_GARDEN`,`GIFTS_AND_PARTIES`,`HEALTH`,`STUDIES`,`HOUSING`,`PERSONAL`,`KIDS`,`CHRISTMASS`,`MOTOCYCLE_BIKE`,`WEDDING` |
| 2     | targetAmount            | AMOUNT    | O        | Yes      | Target amount of Saving account (_embedded AMOUNT type)                                                                                                            | Fields value, precision, currency                                                                                                                                                  |
| 2     | targetDate              | DATE      | O        | Yes      | Target date for saving goal for saving account or termination date for term deposit                                                                                | ISO Date format                                                                                                                                                                    |
| 2     | startDate               | DATE      | O        | No       | Saving start date - date of the first credit on account or opening date of saving account                                                                          | ISO Date format                                                                                                                                                                    |
| 2     | regularDeposit          | AMOUNT    | O        | Yes      | Regular deposit amount, which is automatically credited (by BE system on saving date) on Saving account from linked current account (_embedded AMOUNT type)        | Fields value, precision, currency                                                                                                                                                  |
| 2     | nextProlongation        | DATE      | O        | No       | The next prolongation date, when BE system will automatically credit regular deposit amount on saving account or calculate and transfer interest on term deposit   | ISO Date format                                                                                                                                                                    |
| 2     | interestRateLimit       | AMOUNT    | O        | No       | Limit amount for basic credit Interest rate used for some saving accounts (_embedded AMOUNT type)                                                                  | Fields value, precision, currency                                                                                                                                                  |
| 2     | interestRateOverLimit   | FLOAT     | O        | No       | Credit Interest rate for balance over limit used for some saving accounts                                                                                          |                                                                                                                                                                                    |
| 2     | minimumBalance          | AMOUNT    | O        | No       | Minimum balance amount used only for some saving accounts (_embedded AMOUNT type)                                                                                  | Fields value, precision, currency                                                                                                                                                  |
| 2     | ownTransferMinimum      | AMOUNT    | O        | No       | Minimum amount of own transfer from saving to current account used only for some saving accounts (_embedded AMOUNT type)                                           | Fields value, precision, currency, e.g. 30% of saving disposable balance in SLSP                                                                                                   |
| 2     | ownTransferMaximum      | AMOUNT    | O        | No       | Maximum amount of own transfer from saving to current account used only for some saving accounts (_embedded AMOUNT type)                                           | Fields value, precision, currency, e.g. saving account disposable balance minus minimal balance (which should remain on account) in SLSP                                           |
| 2     | extraSavingMaximum      | AMOUNT    | O        | No       | Maximum amount of own transfer from current to saving account used only for some saving accounts (_embedded AMOUNT type)                                           | Fields value, precision, currency, e.g. sum of extra saving amount could be limited per some period (like 5000 Eur per product in year in SLSP), so extraSavingMaximum is limit minus already executed extra savings in this year |
| 2     | termDeposit             | structure | O        | No       | Structure for special Saving accounts of subtype TERM_DEPOSIT                                                                                                      |                                                                                                                                                                                    |
| 3     | termPeriod              | INTEGER   | O        | No       | Term deposit booking period multiplier of standard periodicity (e.g. 2 weeks, 10 days)                                                                             | Value: integer range 1 - n (could be local specific)                                                                                                                               |
| 3     | periodicity             | ENUM      | O        | No       | Standard periodicity of term deposit                                                                                                                               | ENUM values: [DAILY, WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY] (could be local specific)                                                                                     |
| 3     | remainingProlongations  | INTEGER   | O        | No       | Remaining count of prolongations till term deposit termination                                                                                                     | Value: integer range 0 - n, 0 means no more prolongations                                                                                                                          |
| 3     | interestGrossAmount     | AMOUNT    | O        | No       | Calculated Gross interest amount for current period of term deposit (_embedded AMOUNT type)                                                                        | Fields value, precision, currency                                                                                                                                                  |
| 3     | interestBooking         | ENUM      | O        | No       | Interest booking to term deposit or to linked account during prolongation                                                                                          | ENUM values: [TERM_DEPOSIT, LINKED_ACCOUNT]                                                                                                                                        |
| 1     | loan                    | structure | O        | No       | Structure for Loan and Mortgage accounts                                                                                                                           |                                                                                                                                                                                    |
| 2     | loanAmount              | AMOUNT    | O        | No       | Total contracted Loan/Mortgage amount value (_embedded AMOUNT type) (Optional because not provided in AT, could be mandatory in local WebAPI spec)                 | Fields value, precision, currency                                                                                                                                                  |
| 2     | maturityDate            | DATE      | O        | No       | Final maturity date for Mortgage to be repaid                                                                                                                      | ISO date format                                                                                                                                                                    |
| 2     | drawdownAmount          | AMOUNT    | O        | No       | Total Drawdown amount value utilized so far (_embedded AMOUNT type) (Optional because not provided in AT, could be mandatory in local WebAPI spec)                 | Fields value, precision, currency                                                                                                                                                  |
| 2     | remainingLoanAmount     | AMOUNT    | O        | No       | Remaining Loan/Mortgage amount value, Loan amount minus drawdown (_embedded AMOUNT type) (Optional because not provided in AT, could be mandatory in local spec)   | Fields value, precision, currency                                                                                                                                                  |
| 2     | drawdownToDate          | DATE      | O        | No       | The last date for Loan/Mortgage drawdown (Optional because not provided in AT, could be mandatory in local WebAPI spec)                                            | ISO date format                                                                                                                                                                    |
| 2     | outstandingDebt         | AMOUNT    | O        | No       | Unpaid outstanding amount value, debt (principal+interest) for one lumpsum loan repayment (_embedded AMOUNT type)                                                  | Fields value, precision, currency                                                                                                                                                  |
| 2     | nextRateAmount          | AMOUNT    | O        | No       | Next Installment/Rate amount value (_embedded AMOUNT type) (Optional because not provided in AT, could be mandatory in local WebAPI spec)                          | Fields value, precision, currency                                                                                                                                                  |
| 2     | installmentFrequency    | ENUM      | O        | No       | Installment time period frequency value (Optional because not provided in AT, could be mandatory in local specification)                                           | ENUM values: [WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, 3_YEARLY, 5_YEARLY, IRREGULAR]                                                                                       |
| 2     | installmentDay          | INTEGER   | O        | No       | Day in the month for installment payment                                                                                                                           | Value: integer between 1 - 31                                                                                                                                                      |
| 2     | nextRateDate            | DATE      | O        | No       | Next Installment/Rate due date                                                                                                                                     | ISO date format                                                                                                                                                                    |
| 2     | nextRateNumber          | INTEGER   | O        | No       | Sequence number of the next Installment/Rate payment                                                                                                               |                                                                                                                                                                                    |
| 2     | nextRateType            | TEXT      | O        | No       | Next Installment/Rate type as localized description                                                                                                                |                                                                                                                                                                                    |
| 2     | interestRateToDate      | DATE      | O        | No       | Current interest rate is valid/fixed to this date                                                                                                                  | ISO date format                                                                                                                                                                    |
| 2     | overdueDebt             | AMOUNT    | O        | No       | Overdue debt is sum of unpaid installment amounts after due date (_embedded AMOUNT type)                                                                           | Fields value, precision, currency                                                                                                                                                  |
| 2     | extraRepaymentMaximum   | AMOUNT    | O        | No       | Maximum amount (loan repayment) of own transfer from current to loan account used only for some loan accounts (_embedded AMOUNT type)                              | Fields value, precision, currency, e.g. sum of extra repayment amount could be limited per some period (like 10% of loan amount in year), so extraRepaymentMaximum is repayment limit minus already executed extra repayments in this year |
| 1     | subaccounts             | ARRAY of  | O        | No       | Array of structures for linked subaccounts                                                                                                                         |                                                                                                                                                                                    |
| 2     | id                      | TEXT      | M        | No       | Internal ID as reference for subaccount                                                                                                                            |                                                                                                                                                                                    |
| 2     | accountno               | ACCOUNTNO | O        | No       | Account number of subaccount (_embedded ACCOUNTNO type)                                                                                                            |                                                                                                                                                                                    |
| 2     | type                    | ENUM      | M        | No       | Product Type of account (Current, Saving, Loan).                                                                                                                   | ENUM values: [CURRENT, SAVING, LOAN]                                                                                                                                               |
| 2     | subtype                 | TEXT      | O        | No       | Product Subtype of account, categorization of products below particular type (this local specific value could be used to define FE behaviour of product group).    | Possible values, for type=CURRENT: `CURRENT_ACCOUNT`, `GIRO_ACCOUNT`, `FOREIGN_ACCOUNT`; for type=SAVING: `SAVING_ACCOUNT`, `TERM_DEPOSIT`, `SAVING_PLUS`; for type=LOAN: `LOAN_ACCOUNT`, `MORTGAGE` |
| 2     | product                 | TEXT      | M        | No       | Product name of subaccount. Localization of expected values and George FE behavior is needed, because Erste Group doesn't use harmonized group product definitions.| Values in CSAS: `kasicka`, ...                                                                                                                                                     |
| 2     | productI18N             | TEXT      | M        | No       | Localized product name of subaccount depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language). |                                                                                                                                                                                    |
| 2     | balance                 | AMOUNT    | M        | No       | Account balance for Current, Saved amount for Saving, Principal Outstanding for Loan/Mortgage. (_embedded AMOUNT type)                                             | Fields value, precision, currency                                                                                                                                                  |
| 2     | creditInterestRate      | FLOAT     | O        | No       | Basic credit Interest rate, used for Current and Saving subaccount                                                                                                 | Value in percentage, e.g. 0,5 will be displayed as 0,5 %                                                                                                                           |
| 1     | ownTransferReceivers    | ARRAY of  | O        | No       | Array of structures for own transfer restricted (only available) receiver accounts                                                                                 |                                                                                                                                                                                    |
| 2     | id                      | TEXT      | M        | No       | Internal ID as reference for available receiver account                                                                                                            |                                                                                                                                                                                    |
| 2     | accountno               | ACCOUNTNO | M        | No       | Account number of available receiver account (_embedded ACCOUNTNO type)                                                                                            |                                                                                                                                                                                    |
| 1     | usableFor               | FEATURES  | O        | No       | Array of optional Features that this account is capable of, this array is provided only when specifically requested by using usableFor query parameter             | Features values - see table below                                                                                                                                                  |
| 1     | flags                   | FLAGS     | O        | Yes      | Array of optional Flag values depends on account type, the absence of a certain string is considered as “false”                                                    | Flags values - see table below                                                                                                                                                     |

*Note: New subaccounts structure should be used only in special cases, when subaccount is not provided as whole account object, but only some subaccount attributes should be presented together with main account information (e.g. Special "Kasicka" subaccount product in CSAS.). This structure could be used also for account linkage if required in future.*

*Note: Linked services to account are not part of group account resource. Special separated endpoint for my/accounts/{id}/services could be prepared in Local Spec.*

*Note: Linked cards information is also not provided in account resource, but another existing endpoints for linkage account to card will be used in George FE to display such information on account's screen.*

*Note: Installment and limits information for shadow Credit Card's account (requested in CZ US) is not part of group account structure, because different Card's endpoints should be used to get this information.*

The following flags can be applied to field *flags* in **Account** resource:

Flag                            | Description
------------------------------- | -----------------------------------------------
`accountQueryAllowed`           | User may see the transaction list for this account ("Kontoabfrage").
`ownTransferAllowed`            | Own transfer (between accounts of the same user) is allowed from account with this flag.
`domesticTransferAllowed`       | Domestic payment is allowed from account with this flag.
`internationalTransferAllowed`  | Foreign payment is allowed from account with this flag.
`urgentTransferAllowed`         | Urgent transfers is allowed from account with this flag ("Eilueberweisung").
`electronicStatementAllowed`    | User may see the electronic statements list and download statement for this account. (Flag is not used in AT where statements are always available for account)
`offline`                       | Account is offline (no transactions can be requested), but still visible to the user. e.g. account is currently `closing`
`owner`                         | Current user is owner of the account.
`collectiveSigning`             | Indicates if orders sended from this account have to be signed by at least two users
`dcsAllowed`                    | Account is enabled for "Data Carrier Service".
`cancelled`                     | Account is already cancelled (no transactions can be requested), but still visible to the user for short period (e.g. 5 days after cancel in SLSP) 
`individualConditionsAllowed`   | Individual conditions for international payments are allowed from account with this flag (checkbox on International payment form is displayed).

The following features (could be local specific) can be provided (only when usableFor parameter used in request) to field *usableFor* in **Account** resource:

Features                        | Description
------------------------------- | -----------------------------------------------
`savingsOpening`                | Account may be used to open a savings product.
`blueCodeActivation`            | BlueCode can be activated on this account. (AT local specific so far)
`blueCodeDeactivation`          | BlueCode can be deactivated on this account. (AT local specific so far)

An account can represent one of the following account types and products (values used in AT, local specific values are expected):

| Account.type    | Account.product (defined in AT)
|-----------------|------------------------------
| `CURRENT`       | `girokonto`
| `SAVING`        | `bonuscard`<br>`einlagekonto`<br>`kapitalsparkonto`<br>`praemiensparkonto`<br>`profitcard`<br>`splussparen`<br>`terminsparkonto`<br>`wachstumssparen`
| `LOAN`          | `finanzierung`

+ Parameters
    + id (TEXT) ... internal ID of the account used as part of URI.

+ Model

    + Body

            {
                "id": "CCA4F9863D686D04",
                "accountno": {
                    "iban": "AT712011100000005579",
                    "bic": "GIBAATWWXXX"
                },
                "type": "CURRENT",
                "product": "girokonto",
                "productI18N": "Girokonto",
                "description": "Helmut Fuchs Fuchs description ",
                "alias": "Helmut Fuchs alias",
                "balance": {
                    "value": 111125,
                    "precision": 2,
                    "currency": "EUR"
                },
                "disposable": {
                    "value": 311125,
                    "precision": 2,
                    "currency": "EUR"
                },
                "overdraft": {
                    "value": 200000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "overdraftDueDate": "2016-12-11",
                "creditInterestRate": 0.3,
                "debitInterestRate": 9.0,
                "penaltyInterestRate": 17.67,
                "flags": [
                    "accountQueryAllowed",
                    "ownTransferAllowed",
                    "domesticTransferAllowed",
                    "internationalTransferAllowed",
                    "electronicStatementAllowed"
                ]
            }

### Retrieve single account of user [GET]
Returns the information about one specific account.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Account** resource containing details of one user account identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Account][]

### Update single account [PUT]
Allows to change a limited set of account-settings of one specific account. 
Currently only the field *alias* and saving attributes *targetAmount*, *targetDate*, *regularDeposit* can be changed and the following set of *flags* can be removed only (except electronicStatementAllowed which can be also added):

| Flags
|-------------------------------
| `accountQueryAllowed`
| `ownTransferAllowed`
| `domesticTransferAllowed`
| `internationalTransferAllowed`
| `urgentTransferAllowed`
| `electronicStatementAllowed`

Changing flags values and saving attributes must be signed, whereas changes only to *alias* field must not be signed. Both changes together (Removing of flags and changes to *alias* field) must be signed as well.

Even though other (not editable) fields are not stored they must fulfill the validation-criteria of (ACCOUNT)-Object. *Id* in URL, *id* field and *accountno* field in payload: These fields must refer to the same account, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Account** resource with updated details of one user account identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum              | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|------------------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | account                 | Account                | M        | Account object                                         |                                                                |
| 1     | signInfo                | SIGNINFO               | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id             | The provided ID does not exist.
`ID_MISMATCH`    | id             | The given ID in payload doesn’t match to the ID in URI.
`FIELD_INVALID`  | accountno.iban | Invalid IBAN format.
`FIELD_TOO_LONG` | alias          | Length of the provided alias is greater than 35.
`ACCOUNT_PERMISSIONS_CANT_BE_INCREASED` | flags | Permissions can be removed only - it is not allowed to add permissions via API.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "CCA4F9863D686D04",
                "accountno": {
                    "iban": "AT712011100000005579",
                    "bic": "GIBAATWWXXX"
                },
                "type": "CURRENT",
                "product": "girokonto",
                "productI18N": "Girokonto",
                "description": "Helmut Fuchs Fuchs description ",
                "alias": "Helmut Fuchs NEW alias",
                "balance": {
                    "value": 111125,
                    "precision": 2,
                    "currency": "EUR"
                },
                "disposable": {
                    "value": 311125,
                    "precision": 2,
                    "currency": "EUR"
                },
                "overdraft": {
                    "value": 200000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "overdraftDueDate": "2016-12-11",
                "creditInterestRate": 0.3,
                "debitInterestRate": 9.0,
                "penaltyInterestRate": 17.67,
                "flags": [
                    "accountQueryAllowed",
                    "domesticTransferAllowed",
                    "ownTransferAllowed"
                ]
            }

+ Response 200 (application/json)

    + Body

            {
                "account": {
                    "id": "CCA4F9863D686D04",
                    "accountno": {
                        "iban": "AT712011100000005579",
                        "bic": "GIBAATWWXXX"
                    },
                    "type": "CURRENT",
                    "product": "girokonto",
                    "productI18N": "Girokonto",
                    "description": "Helmut Fuchs Fuchs description ",
                    "alias": "Helmut Fuchs NEW alias",
                    "balance": {
                        "value": 111125,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "disposable": {
                        "value": 311125,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "overdraft": {
                        "value": 200000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "overdraftDueDate": "2016-12-11",
                    "creditInterestRate": 0.3,
                    "debitInterestRate": 9.0,
                    "penaltyInterestRate": 17.67,
                    "flags": [
                        "accountQueryAllowed",
                        "domesticTransferAllowed",
                        "ownTransferAllowed"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016001234"
                }
            }

### Delete single account [DELETE]
Deactivates a specific account. Deactivation of an account has to be signed.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of DELETE resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id             | The provided ID does not exist.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    + Body

            {
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016001235"
                }
            }


## AccountList [/netbanking/my/accounts{?size,page,sort,order,type,usableFor}]
Resource Account List represents collection of accounts to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **Account** type items.

Description of **AccountList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | accounts       | ARRAY of Account | O        | Array of accounts accessible by the user (could be empty) (embedded Account resource) |    |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are: `iban`, `description`, `disposable` and `balance`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.
    + type (TEXT, optional) ... This call delivers all types of accounts by default. It is possible to filter for certain product by using the type URI parameter optional comma-separated list of products. (Functionality is currently not used in George)
    + usableFor (TEXT, optional) ... Optional multi-valued list of features that indicates this call should provide additional feature information in usableFor structure in response. Possible values are: SAVINGS_OPENING, BLUECODE.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 2,
                "nextPage": 1,
                "pageSize": 5,
                "accounts": [
                    {
                       "id": "CCA4F9863D686D04",
                        "accountno": {
                            "iban": "AT622011100000000018",
                            "bic": "GIBAATWWXXX"
                        },
                        "type": "CURRENT",
                        "product": "girokonto",
                        "productI18N": "Girokonto",
                        "description": "Mag. A. M. Mittermuster oder Felix ",
                        "alias": "account alias 1",
                        "balance": {
                            "value": 386776,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "disposable": {
                            "value": 586776,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "overdraft": {
                            "value": 200000,
                            "precision": 2,
                            "currency": "EUR"
                        },            
                        "creditInterestRate": 0.3,
                        "debitInterestRate": 9.0,
                        "penaltyInterestRate": 17.67,
                        "flags": [
                            "accountQueryAllowed",
                            "collectiveSigning",
                            "domesticTransferAllowed",
                            "internationalTransferAllowed",
                            "ownTransferAllowed",
                            "urgentTransferAllowed"
                        ]
                    },
                    {
                        "id": "CCA4F9863D686D03",
                        "accountno": {
                            "iban": "AT402011100000000026",
                            "bic": "GIBAATWWXXX"
                        },
                        "type": "CURRENT",
                        "product": "girokonto",
                        "productI18N": "Girokonto",
                        "description": "Stefan Maier Name2",
                        "alias": "my rich account",
                        "balance": {
                            "value": 1504698,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "disposable": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "flags": [
                            "accountQueryAllowed",
                            "domesticTransferAllowed",
                            "internationalTransferAllowed",
                            "ownTransferAllowed",
                            "urgentTransferAllowed"
                        ]
                    },
                    {
                        "id": "CCA4F9863D686D05",
                        "accountno": {
                            "iban": "AT712011100000005579",
                            "bic": "GIBAATWWXXX"
                        },
                        "type": "LOAN",
                        "subtype": "MORTGAGE",
                        "product": "finanzierung",
                        "productI18N": "Finanzierung konto",
                        "description": "Stefan Maier loan",
                        "alias": "my home mortgage",
                        "balance": {
                            "value": -11504698,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "debitInterestRate": 2.99,
                        "loan": {
                            "loanAmount": {
                                "value": 17000000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "maturityDate": "2035-12-12",
                            "drawdownAmount": {
                                "value": 15000000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "remainingLoanAmount": {
                                "value": 2000000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "drawdownToDate": "2015-12-01",
                            "nextRateAmount": {
                                "value": 128000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "installmentFrequency": "MONTHLY",
                            "installmentDay": 16,
                            "nextRateDate": "2014-12-17T00:00:00+02:00",
                            "nextRateNumber": 7,
                            "interestRateToDate": "2017-12-01"
                        },
                        "flags": [
                            "accountQueryAllowed"
                        ]
                    },
                    {
                        "id": "34839653",
                        "accountno": {
                            "number": "19-0000123457",
                            "bankCode": "0800",
                            "countryCode": "CZ"
                        },
                        "type": "SAVING",
                        "product": "EG.4511611002569547.5",
                        "productI18N": "Sporici úcet CS",
                        "description": "",   // now is always empty or null
                        "alias": "muj sporko",
                        "balance": {
                            "value": 12617140,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "disposable": {
                            "value": 12617140,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "creditInterestRate": 1.75,
                        "saving": {
                            "targetAmount": {
                                "value": 40000000,
                                "precision": 2,
                                "currency": "CZK"
                            },
                            "savingGoal": "CARS_AND_ACCESSORIES",
                            "interestRateLimit" : {
                                "value": 25000000,
                                "precision": 2,
                                "currency": "CZK"
                            },
                            "interestRateOverLimit": 0.5
                        },
                        "flags": [
                            "owner",
                            "accountQueryAllowed",
                            "ownTransferAllowed",
                            "domesticTransferAllowed"
                        ]
                    },
                    {
                        "id": "222234839653",
                        "accountno": {
                            "number": "123",
                            "bankCode": "0800",
                            "countryCode": "CZ"
                        },
                        "type": "CURRENT",
                        "product": "EG.451161100256.8",
                        "productI18N": "Bezni ucet CS s Kasickou",
                        "description": "",   // now is always empty or null
                        "alias": "muj CC ucet s kasickou",
                        "balance": {
                            "value": 5617170,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "disposable": {
                            "value": 5617170,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "creditInterestRate": 0.05,
                        "subaccounts": [
                            {
                                "id": "222234839653-K",
                                "accountno": {
                                    "number": "19-0000000123",
                                    "bankCode": "0800",
                                    "countryCode": "CZ"
                                },
                                "type": "SAVING",
                                "product": "EG.451161100256.KAS",
                                "productI18N": "Sporici ucet CS - Kasicka",
                                "balance": {
                                    "value": 6382830,
                                    "precision": 2,
                                    "currency": "CZK"
                                },
                                "creditInterestRate": 1.99
                            }
                        ],
                        "flags": [
                            "owner",
                            "accountQueryAllowed",
                            "ownTransferAllowed",
                            "domesticTransferAllowed"
                        ]
                    }
                ]
            }

### Get a list of accounts or products for current user [GET]
Get possibly empty list of all accounts, products this user owns. This call is paginated and can be sorted.
Per default this call delivers all types of accounts. It is possible to filter for certain product by using the *type* URI parameter.

**Note:** Closed accounts could be displayed too, because BackEnd provides this information 90 days after account closing.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **AccountList** with possibly empty (omitted) array of *embedded* **Account** items without transaction data.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AccountList][]


## AccountAddress [/netbanking/my/accounts/{id}/address]
Account Address resource represents address associated with particular account used in payment order data.

Description of **AccountAddress** resource attributes: 

| Level | Attribute name | Type/Enum | Mand/Opt | Attribute description                                                                            | Expected values/format     |
|-------|----------------|-----------|----------|--------------------------------------------------------------------------------------------------|----------------------------|
| 1     | street         | TEXT35    | M        | Street and street number used in one text field in payment order address line                    |                            |
| 1     | zipCodeCity    | TEXT35    | M        | ZIP Code and city name used in one text field in payment order address line                      |                            |
| 1     | country        | TEXT35    | M        | Country code (i.e. AT for Austria). Might not be ISO3166 in isolated cases (i.e. Australia AUS). |                            |

+ Parameters
    + id (TEXT) ... internal ID of the account used as part of URI.

+ Model

    + Body

            {
                "street": "Laudongasse 17",
                "zipCodeCity": "1010 Wien",
                "country": "AT"
            }

### Retrieve only account's associated address [GET]
Returns one specific account address information and is used in payment orders processing.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **AccountAddress** resource containing address data of one user account identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.
    

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AccountAddress][]


## AccountTransaction [/netbanking/my/accounts/{id}/transactions/{tId}]
Account Transaction resource represents one single transaction (identified by TID) booked on account identified by account ID. This transaction refers to single account statement line and covers also debit card transaction.

Description of all possible **AccountTransaction** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                        | Expected values/format                                                                            |
|-------|-------------------|-----------------|----------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 1     | id                | ID              | M        | No       | Internal identifier of transaction provided by BE                                                                                                                                            |                                                                                                   |
| 1     | timestampId       | TEXT            | M        | No       | Internal backend specific id to uniquely identify transactions                                                                                                                               |                                                                                                   |
| 1     | referenceId       | TEXT            | O        | No       | Transaction reference ID (Ersterfassungsreferenz) provided by BE when payment order was executed (like document number for transaction)                                                      |                                                                                                   | 
| 1     | orderType         | ENUM            | O        | No       | Payment order type (outgoing payment, outgoing direct debit, incoming payment, incoming direct debit) determines transaction processing in BE.                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT, PAYMENT_IN, DIRECT_DEBIT_IN]                         |
| 1     | state             | ENUM            | M        | No       | State of transaction presented to user on FE                                                                                                                                                 | ENUM values: [CLOSED]                                                                             |
| 1     | sender            | ACCOUNTNO       | O        | No       | Account number of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                        | IBAN format for AT, SK, local bank number for CZ                                                  |
| 1     | senderName        | TEXT            | O        | No       | Name of sender, who created payment order (value provided by BE)                                                                                                                             |                                                                                                   |
| 1     | senderReference   | TEXT            | C        | No       | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                             |                                                                                                   |
| 1     | symbols           | structure       | C        | No       | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####. Fields will by parsed from Sender Reference matching corresponding symbols.) |                                             |
| 2     | variableSymbol    | TEXT            | O        | No       | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                        |                                                                                                   |
| 2     | specificSymbol    | TEXT            | O        | No       | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                        |                                                                                                   |
| 2     | constantSymbol    | TEXT            | O        | No       | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                          |                                                                                                   |
| 1     | receiver          | ACCOUNTNO       | M        | No       | Account number of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory bank code or free text account with bank code/BIC with country code)  |                                                                                                   |
| 1     | receiverName      | TEXT            | O        | No       | Name of receiver of payment order                                                                                                                                                            |                                                                                                   |
| 1     | amount            | AMOUNT          | M        | No       | Booked amount on account in account currency, value with minus if debit on account (embedded AMOUNT type)                                                                                    |                                                                                                   |
| 1     | amountSender      | AMOUNT          | O        | No       | Original transaction amount in defined currency and with precision (embedded AMOUNT type). Must be provided when transaction currency and account currency are different.                    |                                                                                                   |
| 1     | bookingDate       | DATETIME        | M        | No       | Booking/accounting date in BE system in the moment of booking of transaction                                                                                                                 | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                        |
| 1     | valuationDate     | DATETIME        | M        | No       | Valuation date of transaction (could be in the past/future versus booking date), validity date of exchange rates in the case of conversion                                                   | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                        |
| 1     | timestamp         | DATETIME        | M        | No       | The insert timestamp of transaction (could be used to re-sort within booking date)                                                                                                           | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                        |
| 1     | additionalTexts   | structure       | O        | No       | Transaction Additional info structure. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                 |                                                                                                   |
| 2     | lineItems         | ARRAY of TEXT   | O        | No       | Array of additional text fields, usually 4x 35 characters from BE. Payment description, message for receiver.                                                                                |                                                                                                   |
| 1     | paymentReference  | TEXT            | O        | No       | Payment reference used to identify transaction on receiver side. (Used only for SEPA payments)                                                                                               |                                                                                                   |
| 1     | note              | TEXT140         | O        | Yes      | Personal, user specific note to transaction, which could be added to transaction via FE and stored in BE (API)                                                                               |                                                                                                   |
| 1     | bookingType       | TEXT            | O        | No       | Booking type of transaction, BE specific domain value (ZV Auftragsart)                                                                                                                       | Values should be specified in local API documentation                                             |
| 1     | role              | ENUM            | M        | No       | User account side of transaction: sender - for Payment OUT, Direct debit OUT, receiver - otherwise                                                                                           | ENUM values: [SENDER, RECEIVER]                                                                   |
| 1     | transactionType   | ENUM            | M        | No       | Transaction type of transaction, PFM specific domain value (local core banking system values consolidated to PFM defined grouping)                                                           | ENUM values: [CARD_PAYMENT, CASH_DEPOSIT, CASH_WITHDRAWAL, CHARGES, DOMESTIC_TRANSFER, FOREIGN_TRANSFER, INTEREST_TRANSFER, LOAN_DISBURSEMENT, OTHER, SAVINGS_TERM_DEPOSIT, SECURITIES_TERMINATION, SECURITIES_TRANSACTION, STANDING_ORDER, TAXES] |
| 1     | paymentType       | ENUM            | O        | No       | Payment type of transaction: credit - income on account, debit - expense, no booking transaction - no change of account balance                                                              | ENUM values: [CREDIT, DEBIT, NO_CHANGE]                                                           |
| 1     | pfmCategory       | structure       | O        | No       | Optional PFM category structure stored in BE                                                                                                                                                 |                                                                                                   |
| 2     | categoryId        | TEXT            | M        | No       | Internal PFM category ID                                                                                                                                                                     |                                                                                                   |
| 2     | categoryName      | TEXT            | M        | No       | Localized PFM category name                                                                                                                                                                  |                                                                                                   |
| 1     | branding          | structure       | O        | No       | Optional PFM branding structure stored in BE                                                                                                                                                 |                                                                                                   |
| 2     | brandName         | TEXT            | M        | No       | Internal PFM brand Name                                                                                                                                                                      |                                                                                                   |
| 2     | brandUrl          | TEXT            | M        | No       | Internal PFM branch URL                                                                                                                                                                      |                                                                                                   |
| 1     | cardNumber        | TEXT            | O        | No       | Masked card number (PAN - primary account number), only the first 6 and the last 4 digits are displayed, asterisk is used for the rest of digits. Only for Debit/Bank Card transactions.     | ISO 7812 format: "525405******1234"                                                               |
| 1     | mccCode           | TEXT            | O        | No       | Optional identification code of merchant. Applicable only for Card transactions, online payments.                                                                                            |                                                                                                   |
| 1     | merchantName      | TEXT            | O        | No       | Optional information about merchant name related to mcc code.                                                                                                                                |                                                                                                   |
| 1     | location          | TEXT            | O        | No       | Optional information about location where transaction was executed.                                                                                                                          |                                                                                                   |
| 1     | transactionFee    | TEXT            | O        | No       | Optional information about transaction fee.                                                                                                                                                  |                                                                                                   |
| 1     | conversionFee     | TEXT            | O        | No       | Optional information about conversion fee.                                                                                                                                                   |                                                                                                   |
| 1     | exchangeRate      | TEXT            | O        | No       | Optional information about exchange rates used for transaction booking.                                                                                                                      |                                                                                                   |
| 1     | transactionDate   | DATETIME        | O        | No       | Transaction date (used mainly for cards, when debit card was processed in ATM, POS, internet)                                                                                                | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                        |
| 1     | creditorId        | TEXT            | O        | No       | Optional SEPA creditor ID (CID) used with SEPA Direct Debit.                                                                                                                                 |                                                                                                   |
| 1     | mandateId         | TEXT            | O        | No       | Optional SEPA mandate ID (contract between Creditor and payer) used with SEPA Direct Debit.                                                                                                  |                                                                                                   |
| 1     | mandateDate       | DATE            | O        | No       | Optional SEPA mandate signing date (contract between Creditor and payer) used with SEPA Direct Debit.                                                                                        | ISO date format:  YYYY-MM-DD                                                                      |
| 1     | channelId         | ENUM            | O        | No       | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                   | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, BACKEND, ATM, POS, E_COMMERCE, POST_OFFICE, UNKNOWN]  |
| 1     | applicationId     | ENUM            | O        | No       | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                           | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, CARD_CONTROL, TRANSACTIONS, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, ATM_LOCAL, ATM_EB_GROUP, ATM_OTHER, ATM_PAYMENT, POS_LOCAL, POS_EB_GROUP, POS_OTHER, E_PAYMENT, DONATION, POST_OFFICE, UNKNOWN] |
| 1     | flags             | FLAGS           | O        | Yes (hasStar only) | Array of optional Flag values, if not present then flag value is considered as false.                                                                                              | FLAGS: `hasNote`, `hasStar`, `hasVoucher`, `hasPrintableVoucher`, `canceled` (storno)             |

+ Parameters
    + id (TEXT, required) ... ID internal identifier of account used as part of URI.
    + tId (TEXT, required) ... internal transaction identifier used as part of URI.

+ Model

    + Body

            {
                "id": "CCBD429926DBA804UTC20140218230000000",
                "timestampId": "CCBD429926DBA804UTC20140218230000000201402191616514988710100",
                "referenceId": "209991402192AB3-DA2007000037",
                "orderType": "PAYMENT_OUT",
                "state": "CLOSED",
                "sender": {
                    "iban": "AT352011100000003608",
                    "bic": "GIBAATWWXXX"
                },
                "senderName": "Visa-Elisabeth Netbanking",
                "senderReference": "my ref 555",
                "receiver": {
                    "iban": "AT961100000976007260",
                    "bic": "BKAUATWWXXX"
                },
                "receiverName": "Max Mustermann",
                "amount": {
                    "value": -250,
                    "precision": 2,
                    "currency": "EUR"
                },
                "bookingDate": "2014-02-18T23:00:00Z",
                "valuationDate": "2014-02-18T23:00:00Z",
                "timestamp": "2014-02-19T15:16:51Z",
                "additionalTexts": {
                    "lineItems": [
                        "payment description",
                        "info for receiver"
                    ]
                },
                "paymentReference": "PayRef 754786-2014",
                "note": "Already added personal comment",
                "bookingType": "DAABSCH",
                "role": "SENDER",
                "transactionType": "DOMESTIC_TRANSFER",
                "paymentType": "DEBIT",
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE",
                "flags": [
                    "hasVoucher", "hasStar"
                ]
            }

### Get a one single transaction [GET]
Returns the information about one specific transaction on selected account.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Accept**: since application/vnd.at.spardat.rest.netbanking.model.transaction.v1+json.

#### Reply
**AccountTransaction** resource containing details of one transaction identified by parameter TID on account identified by ID.

#### Error codes
Error code      | Scope    | Purpose
----------------|----------|------------------------------------
`ID_NOT_FOUND`  | id       | The provided account ID does not exist.
`ID_NOT_FOUND`  | tId      | The provided transaction ID does not exist.
`VALUE_INVALID` | tId      | Transaction ID is invalid or bookingDate is missing for transaction

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AccountTransaction][]

### Add/Change note and mark single transaction [PUT]
Allows to add or change a client's personal transaction note and mark the transaction as favorite for one specific transaction on selected account. The existing note will be removed, if the given payload has an empty or missing *note* attribute. If *hasStar* flag is provided in input payload, transaction is marked as favorite, otherwise the existing flag will be removed.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

JSON payload in request consists of *note* and *flags* attributes or entire AccountTransaction object could be provided. 

#### Reply
Reply will consists of transaction structure with *id*, *note* and *flags* attributes or entire **AccountTransaction** resource containing details of one transaction identified by parameter TID on account identified by ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT response attributes:

| Level | Attribute name          | Type/Enum       | Mand/Opt | Attribute description                               | Expected values/format                                         |
|-------|-------------------------|-----------------|----------|-----------------------------------------------------|----------------------------------------------------------------|
| 1     | transaction             | structure       | M        | AccountTransaction object or its subset             |                                                                |
| 2     | id                      | ID              | M        | Internal identifier of transaction provided by BE   |                                                                |
| 2     | note                    | TEXT140         | O        | Personal, user specific note to transaction         |                                                                |
| 2     | flags                   | FLAGS           | M        | Array of optional Flag values                       | FLAGS: `hasNote`, `hasStar`, `canceled` (storno)               |
| 1     | signInfo                | SIGNINFO        | M        | SignInfo Details                                    |                                                                |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id (URI)       | The provided account ID does not exist.
`ID_NOT_FOUND`   | tid            | The provided transaction TID does not exist.
`VALUE_INVALID`  | tid            | Transaction TID is invalid or bookingDate is missing for transaction.
`ID_MISMATCH`    | tid (URI), id  | The provided transaction TID (URI) differs from the transaction ID in payload.
`VALUE_INVALID`  | appId          | The resource is only available for georgeclient and quickcheck clients.
`FIELD_TOO_LONG` | note           | Length of the provided note is greater than 140.
`NOT_POSSIBLE`   | note           | A host failure occurred during create/update/delete a note.
`NOT_POSSIBLE`   | flags          | A host failure occurred during adding/removing a hasStar flag.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    + Body

            {
                "note": "New client's personal comment for transaction",
                "flags": [
                    "hasVoucher", "hasStar"
                ]
            }

+ Response 200 (application/json)

    + Body

            {
                "transaction": {
                    "id": "CCBD429926DBA804UTC20140218230000000",
                    "note": "New clients personal comment for transaction",
                    "flags": [
                        "hasVoucher", "hasStar"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "045971701790000016001235"
                }
            }


## AccountTransactionList [/netbanking/my/accounts/{id}/transactions{?sinceId,maxId,size,enablePfm}]
Resource Account Transaction List represents collection of transactions booked on account identified by ID. One transaction refers to single account statement line.
This resource consists of paging attributes and array of *embedded* **AccountTransaction** resource items.

Description of **AccountTransactionList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                          | Expected values/format   |
|-------|----------------|------------------|----------|--------------------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Returned page number, always 0 due to scrolling used instead of paging         | 0 as default             |
| 1     | nextPage       | INTEGER          | M        | Next page value: 1 - if there is more transactions for scrolling, 0 - if not   | 0 or 1                   |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size (value 100 for optimal performance)            |                          |
| 1     | transactions   | ARRAY of AccountTransaction | O   | Array of account transactions (could be empty) (embedded AccountTransaction resource) |                          |

+ Parameters
    + id (TEXT, required) ... internal ID of the account used as part of URI.
    + sinceId (TEXT, optional) ... Optional parameter could be set as the timestampId of the most current transaction in the already fetched transactions list, always sorted by bookingDate. If set then a list of more recent transactions starting from timestampId will be returned if available. This parameter allows a delta call to fetch newer transactions, scrolling forward.
    + maxId (TEXT, optional) ... Optional parameter could be set as the timestampId of the oldest transaction in the already fetched transactions list always sorted by bookingDate. This parameter allows to scroll back in the list of transactions.
    + size (INTEGER, optional) ... Page size used as URI parameter means the max number of transactions to fetch from backend in one call. The optimal size in meanings of performance is 100. Setting less then 100 doesn't provide any performance improvement. Requesting more then 100 decreases performance.
    + enablePfm (TEXT, optional) ... Optional flag. If set to `true`, then BE personal finance manager features will be used for this request (optional URI parameter, default value is `false`).

+ Model

    + Body

            {
                "pageNumber": 0,
                "nextPage": 1,
                "pageSize": 100,
                "transactions": [
                    {
                        "id": "ACBD429926DBA804UTC20140220230000000",
                        "timestampId": "ACBD429926DBA804UTC20140218230000000201402211616514988710200",
                        "referenceId": "209991402202AB3-DA2007000033",
                        "orderType": "PAYMENT_OUT",
                        "state": "CLOSED",
                        "sender": {
                            "iban": "AT352011100000003608",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "Visa-Elisabeth Netbanking",
                        "senderReference": "SO ref 11",
                        "receiver": {
                            "iban": "AT052011100000005603",
                            "bic": "GIBAATWWXXX"
                        },
                        "receiverName": "Max Tester",
                        "amount": {
                            "value": -58256,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bookingDate": "2014-02-20T23:00:00Z",
                        "valuationDate": "2014-02-20T23:00:00Z",
                        "timestamp": "2014-02-21T15:16:51Z",
                        "additionalTexts": {
                            "lineItems": [
                                "Standing order",
                                "Ref 11"
                            ]
                        },
                        "bookingType": "DAABSCH",
                        "role": "SENDER",
                        "transactionType": "STANDING_ORDER",
                        "paymentType": "DEBIT",
                        "channelId": "BACKEND",
                        "flags": [
                            "hasVoucher"
                        ]
                    },
                    {
                        "id": "CCBD429926DBA804UTC20140218230000000",
                        "timestampId": "CCBD429926DBA804UTC20140218230000000201402191616514988710100",
                        "referenceId": "209991402192AB3-DA2007000037",
                        "orderType": "PAYMENT_OUT",
                        "state": "CLOSED",
                        "sender": {
                            "iban": "AT352011100000003608",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "Visa-Elisabeth Netbanking",
                        "senderReference": "my ref 555",
                        "receiver": {
                            "iban": "AT961100000976007260",
                            "bic": "BKAUATWWXXX"
                        },
                        "receiverName": "Max Mustermann",
                        "amount": {
                            "value": -250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bookingDate": "2014-02-18T23:00:00Z",
                        "valuationDate": "2014-02-18T23:00:00Z",
                        "timestamp": "2014-02-19T15:16:51Z",
                        "paymentReference": "PayRef 754786-2014",
                        "note": "Already added personal comment",
                        "bookingType": "DAABSCH",
                        "role": "SENDER",
                        "transactionType": "DOMESTIC_TRANSFER",
                        "paymentType": "DEBIT",
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "flags": [
                            "hasVoucher", "hasStar"
                        ]
                    },
                    {
                        "id": "CCBC009C359B9114UTC20140217230000000",
                        "timestampId": "CCBC009C359B9114UTC20140217230000000201402181616184933430200",
                        "referenceId": "209991402172AIG-132030654494",
                        "orderType": "PAYMENT_IN",
                        "state": "CLOSED",
                        "sender": {
                            "iban": "AT402011100000000026",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "AT402011100000000026",
                        "receiver": {
                            "iban": "AT352011100000003608",
                            "bic": "GIBAATWWXXX"
                        },
                        "receiverName": "Visa-Elisabeth Netbanking",
                        "amount": {
                            "value": 100,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bookingDate": "2014-02-17T23:00:00Z",
                        "valuationDate": "2014-02-13T23:00:00Z",
                        "timestamp": "2014-02-18T15:16:18Z",
                        "additionalTexts": {
                            "lineItems": [
                                "Auftrag bei diesem Konto nicht moeglich",
                                "AT402011100000000026"
                            ]
                        },
                        "bookingType": "RPR-RGS",
                        "role": "RECEIVER",
                        "transactionType": "OTHERS",
                        "paymentType": "CREDIT",
                        "channelId": "BACKEND",
                        "flags": [
                            "hasPrintableVoucher"
                        ]
                    },
                    {
                        "id": "CCB9FF95837DC10CUTC20140216230000000",
                        "timestampId": "CCB9FF95837DC10CUTC20140216230000000201402170201040817410100",
                        "referenceId": "209991402172AB3-DP1025000057",
                        "orderType": "DIRECT_DEBIT_OUT",
                        "state": "CLOSED",
                        "sender": {
                            "iban": "AT352011100000003608",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "Visa-Elisabeth Netbanking",
                        "senderReference": "Direct debit",
                        "receiver": {
                            "iban": "AT022011200000000026",
                            "bic": "GIBAAT21XXX"
                        },
                        "receiverName": "Max von Zweite Wiener Vereins-Sparcasse",
                        "amount": {
                            "value": 12250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bookingDate": "2014-02-16T23:00:00Z",
                        "valuationDate": "2014-02-16T23:00:00Z",
                        "timestamp": "2014-02-17T01:01:51Z",
                        "paymentReference": "Visa-Elisabeth Netbanking Sonst.Zahlungen 17.02.14",
                        "bookingType": "DAABBUCH",
                        "role": "SENDER",
                        "transactionType": "DOMESTIC_TRANSFER",
                        "paymentType": "CREDIT",
                        "creditorId": "AT28ZZZ70000000054",
                        "mandateId": "80912345",
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "flags": [
                            "hasVoucher"
                        ]
                    }
                ]
            }    

### Get a list of transactions for one account [GET]
Get possibly empty list of transactions for account identified by ID. This call is always sorted by *bookingDate*, no other sorts are supported due to performance reasons. This call doesn't use standard paging as other list calls, since it is scrollable (forward and backward) by setting the URI query parameters *maxId* and *sinceId*.

**Note:** Standard Pagination is not working for this request, since it causes penalties on performance. The client calling this endpoint can greatly influcence performance.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Accept**: since application/vnd.at.spardat.rest.netbanking.model.transaction.v1+json.

#### Reply
Resource **AccountTransactionList** with possibly empty (omitted) array of *embedded* **AccountTransaction** items.

#### Error codes
Error code     | Scope          | Purpose
---------------|----------------|------------------------------------
`ID_NOT_FOUND` | id             | The provided account ID does not exist.
`INVALID_DATA` | sinceId, maxId | Parameter combination of maxId and sinceId is not allowed.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
            

+ Response 200 (application/json)

    [AccountTransactionList][]


## AccountReservationList [/netbanking/my/accounts/{id}/reservations]
Resource Account Reservation List represents collection of reservations done on account identified by ID. 
Usual example of one reservation is single debit card authorization done on main account linked to this card or credit card pre-authorization on shadow credit card account. 
Reservation is not real transaction booked on account, but has impact on disposable balance. Reservation could be changed to real transaction after receiving confirmation from Card Management system in few days after card authorization.
This resource consists of paging attributes and array of reservation items.

Description of **AccountReservationList** resource attributes: 

| Level | Attribute name    | Type/Enum     | Mand/Opt | Attribute description                                                                                      | Expected values/format                                    |
|-------|-------------------|---------------|----------|------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| 1     | pageNumber        | INTEGER       | M        | Returned page number, always 0 due to scrolling used instead of paging                                     | 0 as default                                              |
| 1     | pageCount         | INTEGER       | M        | Total number of pages of defined size                                                                      |                                                           |
| 1     | nextPage          | INTEGER       | O        | Page number of following page (provided only when exist)                                                   |                                                           |
| 1     | pageSize          | INTEGER       | M        | Provided or defaulted page size                                                                            |                                                           |
| 1     | reservations      | ARRAY of      | O        | Array of account reservations (could be empty)                                                             |                                                           |
| 2     | creationDate      | DATETIME      | M        | Creation date and time of reservation (mainly when card was processed in ATM, POS, internet).              | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                |
| 2     | expirationDate    | DATETIME      | O        | Expiration date and time of reservation (e.g. when credit card pre-authorization will expire).             | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                |
| 2     | amount            | AMOUNT        | O        | Reserved amount on account in account currency (embedded AMOUNT type)                                      |                                                           |
| 2     | amountSender      | AMOUNT        | M        | Original reservation amount in defined currency and with precision (embedded AMOUNT type).                 |                                                           |
| 2     | description       | TEXT          | M        | Bank specific description of reservation, which could be stored in BE (API) or generated on-line.          |                                                           |
| 2     | mccCode           | TEXT          | O        | Optional identification code of merchant. Applicable only for Card transactions, online payments.          |                                                           |
| 2     | merchantName      | TEXT          | O        | Optional information about merchant name related to mcc code.                                              |                                                           |
| 2     | location          | TEXT          | O        | Optional information about location where transaction was executed.                                        |                                                           |
| 2     | type              | TEXT          | O        | Type of reservation, local BE specific domain value                                                        | Values should be specified in local API documentation     |
| 2     | status            | ENUM          | M        | Reservation status.                                                                                        | ENUM values: [RESERVED, CANCELLED, EXPIRED]               |

+ Parameters
    + id (TEXT, required) ... internal ID of the account used as part of URI.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 10,
                "reservations": [
                    {
                        "creationDate": "2015-08-20T23:00:00+01:00",
                        "expirationDate": "2015-09-07T23:00:00+01:00",
                        "amount": {
                            "value": 58256,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "amountSender": {
                            "value": 58256,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "description": "Card transaction in POS, 97492347",
                        "merchantName": "Billa market",
                        "location": "Brno, CZ",
                        "type": "POS transaction",
                        "status": "RESERVED"
                    },
                    {
                        "creationDate": "2015-08-22T15:17:00+01:00",
                        "amount": {
                            "value": 81074
                            "precision": 2,
                            "currency": "CZK
                        },
                        "amountSender": {
                            "value": 3000
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "description": "FX withdrawal in ATM, 4324524",
                        "location": "Brno, CZ",
                        "type": "ATM withdrawal",
                        "status": "RESERVED"
                    }
                ]
            }    

### Get a list of reservations for one account [GET]
Get possibly empty list of reservations for account identified by ID. This call is always sorted by *creationDate*, no other sorts are supported due to performance reasons.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **AccountReservationList** with possibly empty (omitted) array of reservation items.

#### Error codes
Error code     | Scope          | Purpose
---------------|----------------|------------------------------------
`ID_NOT_FOUND` | id             | The provided account ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
            

+ Response 200 (application/json)

    [AccountReservationList][]


# Group Payments
Payment-related resources of *Banking Services API*.

## Payment [/netbanking/my/orders/payments/{id}]
Payment resource represents one single payment order entered by the user. This payment could be one of the types: *Payment Domestic*, *Payment SEPA*, or *Payment International*.

Description of all possible **Payment** resource attributes: 

| Level | Attribute name      | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|---------------------|-----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order               | structure       | M        | Order structure (needed because of signInfo object attached to payment order)                                                                                                                                                      |                                                                                                                                                                                    |
| 2     | id                  | TEXT            | M        | Internal identifier of payment order (provided as response after payment creation from BE)                                                                                                                                         |                                                                                                                                                                                    |
| 2     | referenceId         | TEXT            | O        | Transaction reference ID provided by BE when payment order was executed                                                                                                                                                            |                                                                                                                                                                                    |
| 2     | orderCategory       | ENUM            | M        | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [DOMESTIC, SEPA, INTERNATIONAL, OWN_TRANSFER, INTRA_BANK]                                                                                                             |
| 2     | orderType           | ENUM            | M        | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT, DIRECT_DEBIT_IN] - incoming DD from partner's account bank                                                                            |
| 2     | state               | ENUM            | M        | State of payment order presented to user on FE, value is mapped based on provided BE technical states.                                                                                                                             | ENUM values: [CREATED, OPEN, SPOOLED, CANCELLED, CLOSED, DELETED]                                                                                                                  |
| 2     | stateDetail         | ENUM            | M        | State detail of payment order provided based on BE technical states. Mapping between technical BE states and predefined FE detail states should be specified by local API. Value is used in FE to display status description.      | ENUM values: [CRE, OPN, INB, TAF, STO, KAG, SNM, NGD, NGA, ADB, AGB, OBG, UNG, BNZ, ENE, TRM, OFL, RPS, CHK, PNR, WER, FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK]      |
| 2     | stateOk             | BOOLEAN         | M        | Indicator whether state (stateDetail value) of payment order is OK from user point of view.                                                                                                                                        | Boolean values: `true`/`false` - For mapping between stateDetail and stateOk indicator values see table below.                                                                     |
| 2     | sender              | ACCOUNTNO       | M        | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName          | TEXT            | O        | Name of sender, who created payment order (value provided by BE)                                                                                                                                                                   |                                                                                                                                                                                    |
| 2     | senderReference     | TEXT            | O        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | symbols             | structure       | O        | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####. Fields will by parsed from Sender Reference matching corresponding symbols.) |                                                                                                                                                                    |
| 3     | variableSymbol      | TEXT            | O        | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                                                              |                                                                                                                                                                                    |
| 3     | specificSymbol      | TEXT            | O        | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                                                              |                                                                                                                                                                                    |
| 3     | constantSymbol      | TEXT            | O        | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                                                |                                                                                                                                                                                    |
| 2     | receiver            | ACCOUNTNO       | M        | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName        | TEXT            | O        | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount              | AMOUNT          | M        | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate        | DATE            | O        | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | modificationDate    | DATETIME        | O        | Modification date indicates the last update of payment order done by user or BE system (read-only field provided by BE)                                                                                                            | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | executionDate       | DATETIME        | O        | Datetime when payment order was created/updated (the last time) by user (read-only field is automatically setup/changed by BE system based on POST/PUT request)                                                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | expirationDate      | DATETIME        | O        | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | repetitionDays      | INTEGER         | O        | Number of days after transfer date till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                     | E.g. integer from interval 1-14 in SK                                                                                                                                              |
| 2     | additionalInfo      | structure       | O        | Payment Additional info structure with optional elements. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                                    |                                                                                                                                                                                    |
| 3     | text4x35            | ARRAY of TEXT   | O        | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 3     | paymentReference    | TEXT            | O        | Payment reference used to identify payment order on receiver side. (Used only for Domestic/SEPA payments)                                                                                                                          |                                                                                                                                                                                    |
| 2     | feeSharingCode      | ENUM            | O        | Transfer Fee sharing code, mandatory for International payment, default value `SHA` for SEPA payment, not used for Domestic payment.                                                                                               | ENUM values: [OUR, SHA, BEN] `OUR` - Transfer fee paid by sender, `SHA` - Transfer fee is shared by both, `BEN` - Transfer fee paid by receiver/beneficiary.                       |
| 2     | senderAccountName   | TEXT            | O        | Name of sender account, payment order is transferred from (value provided by BE) (Used only for SEPA and International payments)                                                                                                   |                                                                                                                                                                                    |
| 2     | senderAddress       | ARRAY of TEXT   | O        | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International payments)                               | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverAddress     | ARRAY of TEXT   | O        | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International payments)                             | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverBankName    | TEXT            | O        | Receiver's Bank name of receiver's account, where payment order is transferred to (value provided by BE) (Used only for International payments)                                                                                    |                                                                                                                                                                                    |
| 2     | receiverBankAddress | ARRAY of TEXT   | O        | Receiver's Bank address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International payments)                        | array of 3x string with max lenght 35 characters each                                                                                                                              |
| 2     | interbank           | BIC             | O        | BIC of in-between (correspondent) bank if used in International transfer. (Used only for International payments)                                                                                                                   |                                                                                                                                                                                    |
| 2     | directDebit         | structure       | O        | Direct Debit info structure used only for SEPA Direct Debit payments.                                                                                                                                                              |                                                                                                                                                                                    |
| 3     | creditorId          | TEXT            | M        | Direct Debit Creditor Identification (SEPA CID).                                                                                                                                                                                   | Predefined SEPA CID format agreed for each SEPA country, e.g. SKccZZZ7nnnnnnnnnn                                                                                                   |
| 3     | creditorName        | TEXT            | M        | Direct Debit Creditor Name, which is linked to CID.                                                                                                                                                                                |                                                                                                                                                                                    |
| 3     | mandateId           | TEXT            | M        | Direct Debit Mandate Identification, ID of contract/mandate between CID and particular debtor.                                                                                                                                     |                                                                                                                                                                                    |
| 3     | mandateDate         | DATE            | O        | Direct Debit Mandate Signature date of contract/mandate between CID and particular debtor.                                                                                                                                         | ISO date format: YYYY-MM-DD                                                                                                                                                        |
| 3     | scheme              | ENUM            | O        | Direct Debit Scheme, value CORE should be used for Retail, B2B (Business-To-Business) is only available to businesses, the payer must not be a private individual (consumer).                                                      | ENUM values: [CORE, B2B], default value is CORE                                                                                                                                    |
| 3     | sequenceType        | ENUM            | M        | Direct Debit Sequence Type, values: 'FRST' - first DD from CID based on particular mandate, 'FNAL' - final DD for mandate, 'RCUR' - recurring DD for provided mandate, 'OOFF' - one off mandate, only one DD.                      | ENUM values: [FRST, FNAL, RCUR, OOFF]                                                                                                                                              |
| 2     | confirmations       | ARRAY of        | O        | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId           | TEXT            | O        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email               | EMAIL           | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language            | ENUM            | M        | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | authorizations      | ARRAY of        | O        | Authorization structure (possible collection) consists of authorization timestamp and name of client who signed payment order                                                                                                      |                                                                                                                                                                                    |
| 3     | signatureTime       | DATETIME        | M        | Datetime when payment order was signed by user (read-only field is automatically setup by BE system based on successful authorization request)                                                                                     | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 3     | clientName          | TEXT            | M        | Full name of client who signed payment order (read-only field is automatically setup by BE system based on successful authorization request)                                                                                       | format "FirstName MiddleName LastName"                                                                                                                                             |
| 3     | userId              | TEXT            | O        | User ID of client who signed payment order (read-only field is automatically setup by BE system based on successful authorization request)                                                                                         |                                                                                                                                                                                    |
| 2     | channelId           | ENUM            | O        | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, BACKEND, ATM, POS, E_COMMERCE, POST_OFFICE, UNKNOWN]                                  |
| 2     | applicationId       | ENUM            | O        | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, CARD_CONTROL, TRANSACTIONS, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, ATM_LOCAL, ATM_EB_GROUP, ATM_OTHER, ATM_PAYMENT, POS_LOCAL, POS_EB_GROUP, POS_OTHER, E_PAYMENT, DONATION, POST_OFFICE, UNKNOWN] |
| 2     | flags               | FLAGS           | O        | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |
| 1     | signInfo            | SIGNINFO        | O        | SignInfo Details, consists of state and signId - Hash value calculated using common relevant payment order attributes to ensure the same unchanged object (payment order) is used every time during authorization signing process  |                                                                                                                                                                                    |

Possible *stateDetail* values used to display relevant status info in FE are defined in following table (local API spec should define mapping to BE technical statuses):
 
stateDetail | Meaning                                       | stateOk
------------|-----------------------------------------------|--------------
`CRE`       | Just created                                  | `true`
`OPN`       | Open - not signed yet                         | `true`
`INB`       | Being processed - in process                  | `true`
`TAF`       | Invalid signature - TAN/TAC wrong             | `false`
`STO`       | Cancelled - storno                            | `false`
`KAG`       | Expired - payment order not processed         | `false`
`SNM`       | Service not possible - payment not processed  | `false`
`NGD`       | Insufficient funds - no disposable balance    | `false`
`NGA`       | Rejected - rejected not covered               | `false`
`ADB`       | Rejected - rejected by advisor (RPS)          | `false`
`AGB`       | Bank code declined - rejected due to bankcode | `false`
`OBG`       | Declined upper limit - rejected by max limit  | `false`
`UNG`       | Declined lower limit - rejected by min limit  | `false`
`BNZ`       | Posting not allowed - transfer not possible   | `false`
`ENE`       | IN payment not allowed - deposit not possible | `false`
`TRM`       | Marked as scheduled payment                   | `true`
`OFL`       | Subsequent attempt - offline payment          | `true`
`RPS`       | Forwarded for clearing - BE approval ready    | `true`
`CHK`       | Order check - checked by spooler RT-Host Ft.  | `true`
`PNR`       | Payment Order from night register             | `true`
`WER`       | Payment Order waiting for setup exchange rate | `true`
`FIN`       | Forwarded - finalized neutral                 | `true`
`FIH`       | Forwarded - finalized today (current bankday) | `true`
`FIM`       | Forwarded - finalized tomorrow (next bankday) | `true`
`FIX`       | Forwarded - finalized without disposing       | `true`
`FIR`       | Forwarded - finalized realtime host           | `true`
`FIK`       | Forwarded - finalized crisis region           | `true`
`FID`       | Forwarded - finalized dirty words             | `true`
`BLA`       | Forwarded - black list                        | `false`
`FUS`       | Unknown - finalized unknown                   | `false`
`ABG`       | Rejected - rejected, ask advisor              | `false`
`UNK`       | Unknown - unknown state, ask advisor          | `false`

API Mapping of *stateDetail* values to *state* values is in following table (local API spec could define mapping to BE technical status):

state       | Meaning
------------|-------------------------------------------------------------------------------------
`CREATED`   | Order state in a CreateState (CRE)
`OPEN`      | Order state in an OpenState (OPN, TAF, KAG, SNM, NGA, ADB, AGB, OBG, UNG, BNZ, ENE)
`SPOOLED`   | Order state in a SpoolState (INB, NGD, TRM, OFL, RPS, CHK, PNR, WER)
`CANCELLED` | Order state in a CancelState (STO)
`CLOSED`    | Order state in a ClosedState (FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK)
`DELETED`   | Order state in a DeleteState (order with `deleted` flag from BE)

The following flags can be applied to field *flags* in **Payment** resource:

Flag                       | Description
---------------------------|-----------------------------------------------
`urgent`                   | Flag indicating urgent payment order (in SEPA, SWIFT and maybe also in local bank clearing systems) requested by client
`editable`                 | Flag indicating if payment order can be edited by client
`deletable`                | Flag indicating if payment order can be deleted by client. Already signed payment order must be canceled before deleting.
`cancelable`               | Flag indicating if future dated (already signed) payment order can be canceled by client
`individualConditions`     | Flag indicating individual conditions for international payment order requested by client
`collective`               | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)
`advanceInterestBonusLost` | This flag is returned if signing this order will (probably) result in lost savings. (Specific for AT only)

+ Parameters
    + id (TEXT) ... ID internal identifier of payment order used as part of URI.

+ Model

    + Body

            {
                "order": {
                    "id": "043721778790000001007101",
                    "referenceId": "TRN-79988998",
                    "orderCategory": "SEPA",
                    "orderType": "PAYMENT_OUT",
                    "state": "CLOSED",
                    "stateDetail": "FIH",
                    "stateOk": true,
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Dkffr. Manfred Dichtl",
                    "receiver": {
                        "iban": "AT961100000976007260",
                        "bic": "BKAUATWWXXX"
                    },
                    "receiverName": "Max Mustermann",
                    "amount": {
                        "value": 250,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "modificationDate": "2014-11-09T05:21:00+02:00",          
                    "executionDate": "2014-11-09T05:21:00+02:00",          
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2014"
                    },
                    "feeSharingCode": "SHA",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "flags": [
                        "editable",
                        "deletable"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "kjdhskh94829jhfdkhfks472984876"
                }
            }

### Get a one single payment order [GET]
Returns the information about one specific (Domestic, SEPA, or International) payment order entered by user based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**Payment** resource containing details of one user payment order identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Payment][]

### Remove one specific payment order [DELETE]
Delete one specific payment order identified by ID.
DELETE method for payment order should be possible in following cases:
- for not signed payment order in state 'CREATED' or 'OPEN', simple delete of manageable payment order
- for already signed payment order in state 'SPOOLED' (should be available only for payment orders flagged as 'cancelable'), we can call it as "cancel of payment in BE process", which requires additional signing process 
- for canceled payment order in state 'CANCELED', just delete of canceled payment (should be available only for payment order flagged as 'deletable') - this case could be as the last step of previous cancel (local specific), that means not available as special call from FE

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentDeleteEntry** input payload containing optional attributes (local specific) to determine payment order category and sender's account (mandatory in CSAS) of particular payment order.

Description of **PaymentDeleteEntry** input request attributes: 

| Level | Attribute name | Type/Enum | Mand/Opt | Attribute description                                                                                                       | Expected values/format                                                 |
|-------|----------------|-----------|----------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| 1     | orderCategory  | ENUM      | O        | Payment order category determines whether payment is domestic, SEPA, international, inside the bank or between own accounts | ENUM values: [DOMESTIC, SEPA, INTERNATIONAL, OWN_TRANSFER, INTRA_BANK] |
| 1     | sender         | ACCOUNTNO | O        | Account number of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local number with mandatory local bankCode)| IBAN format for AT, SK, local bank number for CZ                       |

#### Reply
Expected response depends on mentioned cases when DELETE method is called:
- for not signed payment order in state 'CREATED' or 'OPEN' - response: HTTP 204 without payload
- for already signed payment order in state 'SPOOLED', which requires additional signing process - response: HTTP 200 with mandatory signInfo object
- for canceled payment order in state 'CANCELED' - response: HTTP 204 without payload

Description of DELETE resource attributes (except cancel of payment in BE process):
No content.

#### Error codes
Error code           | Scope    | Purpose
-------------------- | -------- | ----------------------------
`ID_NOT_FOUND`       | id       | The item could not be found.
`STATE_INVALID`      | id       | The order is in a state that does not allows deleting.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "orderCategory": "DOMESTIC",
                "sender": {
                    "number": "19-123",
                    "bankCode": "0800",
                    "countryCode": "CZ"
                }
            }

+ Response 204


## PaymentList [/netbanking/my/orders/payments{?size,page,sort,order,state,channel}]
Resource Payment List represents collection of all payment orders entered by user and not deleted.
This resource consists of paging attributes and array of *embedded* **Payment** resource items.

Description of **PaymentList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | orders         | ARRAY of Payment | O        | Array of payment orders entered by the user (could be empty) (embedded Payment type/resource) |  |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort field is only: `transferDate`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.
    + state (TEXT, optional) ... This call delivers all user payments in all states by default (no state parameter provided). It is possible to filter for certain payments by using the state URI parameter with possible values: `created`, `open`, `spooled`, `closed`, `deleted`. *(Functionality is currently not used in George)*
    + channel (TEXT, optional) ... This call delivers all user payments entered via all channels (no channel parameter provided) or entered by user via specific channel/application (e.g. `George` could be used). It is possible to filter for certain payments by using the channel URI parameter with possible values: `George`, `MobileApp`, `Business24`. *(Functionality is currently not provided in George AT)*

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 2,
                "nextPage": 1,
                "pageSize": 10,
                "orders": [
                    {
                        "order": {
                            "id": "043869409830000411024045",
                            "orderCategory": "INTERNATIONAL",
                            "orderType": "PAYMENT_OUT",
                            "state": "OPEN",
                            "stateDetail": "OPN",
                            "stateOk": true,
                            "sender": {
                                "iban": "AT622011100000000018",
                                "bic": "GIBAATWWXXX"
                            },
                            "senderName": "Mag. A. M. Mittermuster oder Felix",
                            "senderReference": "INV-2014/8584",
                            "receiver": {
                                "iban": "AE060330000010195511161",
                                "bic": "BOMLAEA0",
                                "countryCode": "AE"
                            },
                            "receiverName": "Dubai Friend",
                            "amount": {
                                "value": 125000,
                                "precision": 2,
                                "currency": "USD"
                            },
                            "transferDate": "2014-12-15",
                            "modificationDate": "2014-12-11T13:00:00Z",
                            "executionDate": "2014-12-11T13:00:00Z",
                            "additionalInfo": {
                                "text4x35": [
                                    "Flight to Dubai",
                                    "Fly Emirates 555"
                                ]
                            },
                            "feeSharingCode": "BEN",
                            "senderAccountName": "Felix's Current account",
                            "senderAddress": {
                                "Geiselbergstraße 66",
                                "1010 Wien",
                                "Austria"
                            },
                            "receiverAddress": {
                                "Sheikh Zayed Rd 666",
                                "Dubai",
                                "United Arab Emirates"
                            },
                            "channelId": "NET_BANKING",
                            "applicationId": "GEORGE",
                            "flags": [
                                "editable",
                                "deletable"
                            ]
                        },
                        "signInfo": {
                            "state": "OPEN",
                            "signId": "kjdhskh94829jhfdkhfks472984876"
                        }
                    },
                    {
                        "order": {
                            "id": "043721778790000001007101",
                            "referenceId": "TRN-79988998",
                            "orderCategory": "SEPA",
                            "orderType": "PAYMENT_OUT",
                            "state": "CLOSED",
                            "stateDetail": "FIH",
                            "stateOk": true,
                            "sender": {
                                "iban": "AT482011100000005702",
                                "bic": "GIBAATWWXXX"
                            },
                            "senderName": "Dkffr. Manfred Dichtl",
                            "receiver": {
                                "iban": "AT961100000976007260",
                                "bic": "BKAUATWWXXX"
                            },
                            "receiverName": "Max Mustermann",
                            "amount": {
                                "value": 250,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "modificationDate": "2014-11-09T08:21:00+02:00",          
                            "executionDate": "2014-11-09T05:21:00+02:00",          
                            "additionalInfo": {
                                "paymentReference": "PayRef 754786-2014"
                            },
                            "feeSharingCode": "SHA",
                            "channelId": "NET_BANKING",
                            "applicationId": "GEORGE"
                        },
                        "signInfo": {
                            "state": "DONE"
                        }
                    },
                    {
                        "order": {
                            "id": "043721778790000001007777",
                            "orderCategory": "SEPA",
                            "orderType": "DIRECT_DEBIT_IN",
                            "state": "OPEN",
                            "stateDetail": "OPN",
                            "stateOk": true,
                            "sender": {
                                "iban": "AT961100000976007260"
                            },
                            "senderName": "Austria Telecom",
                            "receiver": {
                                "iban": "AT482011100000005702",
                                "bic": "GIBAATWWXXX"
                            },
                            "receiverName": "Dkffr. Manfred Dichtl",
                            "amount": {
                                "value": 10000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "modificationDate": "2014-11-15T00:00:00+02:00",          
                            "additionalInfo": {
                                "paymentReference": "Direct Debit - Invoice 2014/7463698"
                            },
                            "channelId": "UNKNOWN"
                        },
                        "signInfo": {
                            "state": "OPEN",
                            "signId": "kjdhskh94829jhfdkhfks4729ffdd876"
                        }
                    },
                    {
                        "order": {
                            "id": "CSAS-11024045",
                            "orderCategory": "DOMESTIC",
                            "orderType": "PAYMENT_OUT",
                            "state": "OPEN",
                            "stateDetail": "OPN",
                            "stateOk": true,
                            "sender": {
                                "number": "19-123",
                                "bankCode": "0800",
                                "countryCode": "CZ"
                            },
                            "senderName": "Pepa Travnicek",
                            "symbols": {
                                "variableSymbol": "0123456789",
                                "specificSymbol": "999999"
                            },
                            "receiver": {
                                "number": "123-123",
                                "bankCode": "0100",
                                "countryCode": "CZ"
                            },
                            "receiverName": "Dan Nekonecny",
                            "amount": {
                                "value": 125000,
                                "precision": 2,
                                "currency": "CZK"
                            },
                            "transferDate": "2014-11-14",
                            "modificationDate": "2014-11-11T13:00:00Z",
                            "executionDate": "2014-11-11T13:00:00Z",          
                            "additionalInfo": {
                                "text4x35": [
                                    "no nekup to"
                                ]
                            },
                            "channelId": "MOBILE_BANKING",
                            "applicationId": "GEORGE_GO"
                        },
                        "signInfo": {
                            "state": "OPEN",
                            "signId": "kjdhskh94829jhfdkhfks47298dcecs6"
                        }
                    }
                ]
            }

### Get a list of payment orders for current user [GET]
Get possibly empty list of all payment orders; open, scheduled, pending and closed ones; domestic and international, that have been entered from all user’s accounts through all distribution channels in the last 92 days (configuration parameter). This call is paginated and can be sorted.
This call delivers all payment orders in all states entered via channel "George" as default. It is possible to filter for status of payment orders by using the *state* URI parameter or payments entered via all channels if *channel* URI parameter will be empty.

See possible *state* values in following table:

state       | Meaning
----------- | --------------------
`CREATED`   | Order state in a CreateState (CRE)
`OPEN`      | Order state in an OpenState (OPN, TAF, KAG, SNM, NGA, ADB, AGB, OBG, UNG, BNZ, ENE)
`SPOOLED`   | Order state in a SpoolState (INB, NGD, TRM, OFL, RPS, CHK, PNR, WEX)
`CANCELLED` | Order state in a CancelState (STO)
`CLOSED`    | Order state in a ClosedState (FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK)
`DELETED`   | Order state in a DeleteState (order with `deleted` flag from BE)
none        | If no State is given, the complete list is delivered

**Note:** If a payment order is logically deleted, but still in a valid state, it has to be suppressed and not delivered by the API.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **PaymentList** with possibly empty (omitted) array of *embedded* **Payment** items.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [PaymentList][]


## PaymentBookingDate [/netbanking/my/orders/payments/bookingdate{?accountId}]
Payment Booking Date resource represents current BE booking date available for provided account.

Description of **PaymentBookingDate** resource attributes: 

| Level | Attribute name  | Type/Enum | Mand/Opt | Attribute description                                                                                                                                          | Expected values/format                     |
|-------|-----------------|-----------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| 1     | bookingDate     | DATETIME  | M        | Booking date which will be used in BE system, if payment order from provided account will be signed and processed right now                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ |
| 1     | urgentAvailable | BOOLEAN   | O        | Identifies if urgent payment checkbox can be displayed to user based on orderCategory, currency and current local BE system time and local cutoff time logic   | Boolean values: `true`/`false`             |

+ Parameters
    + accountId (TEXT, optional) ... internal ID of the account used as part of URI. Mandatory in AT.

+ Model

    + Body

            {
                "bookingDate": "2015-03-23T23:00:00Z",
                "urgentAvailable": "true"
            }

### Retrieve current booking date associated with account for defined payment order category [PUT]
Returns current available booking date (today or the next available bank working day) based on the provided account and optional payment order category parameters. This booking date is calculated based on different cutOff times for processing of specified payment category in associated institution (in AT different institutions could have different cutOff times). This booking date will be used during booking in BE system, if such payment order from provided account will be signed and processed right now. Booking day could be the next working/bank day if current day is bank holiday, weekend or time is over the cutOff time for processing such payment order category.
The returned date value is not the delivery date, but it can be used to predict it on FE, if the maximum transfer time is known for particular payment order category.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentBookingDateEntry** input payload containing optional attributes (local specific) to determine booking date available for account payment.

Description of **PaymentBookingDateEntry** input request attributes: 

| Level | Attribute name       | Type/Enum | Mand/Opt | Attribute description                                                                                                       | Expected values/format                                                 |
|-------|----------------------|-----------|----------|-----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| 1     | orderCategory        | ENUM      | O        | Payment order category determines whether payment is domestic, SEPA, international, inside the bank or between own accounts | ENUM values: [DOMESTIC, SEPA, INTERNATIONAL, OWN_TRANSFER, INTRA_BANK] |
| 1     | sender               | ACCOUNTNO | O        | Account number of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local number with mandatory local bankCode | IBAN format for AT, SK, local bank number for CZ                       |
| 1     | receiver             | ACCOUNTNO | O        | Account number of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with local bankCode |                                                                        |
| 1     | currency             | ISO4217   | O        | ISO Currency code                                                                                                           | E.g. `EUR`, `CZK`                                                      |
| 1     | priority             | ENUM      | O        | Payment order priority selected by user                                                                                     | ENUM values: [URGENT, STANDARD]                                        |
| 1     | individualConditions | BOOLEAN   | O        | Indicator whether individual conditions (rate) should be used for payment order in different then local currency            | Boolean values: `true`/`false`                                         |

#### Reply
A **PaymentBookingDate** resource containing one booking date value and optional urgent payment flag for provided account ID and payment order category parameters.

#### Error codes
Error code     | Scope     | Purpose
---------------|-----------|------------------------------------
`ID_NOT_FOUND` | accountId | The provided ID does not exist.
    

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
            
    + Body

            {
                "orderCategory": "DOMESTIC",
                "sender": {
                    "number": "19-123",
                    "bankCode": "0800",
                    "countryCode": "CZ"
                },
                "receiver": {
                    "number": "123-123",
                    "bankCode": "0100",
                    "countryCode": "CZ"
                },
                "currency": "CZK",
                "priority": "STANDARD"
            }

+ Response 200 (application/json)

    [PaymentBookingDate][]


## PaymentDomestic [/netbanking/my/orders/payments/domestic/{id}]
Payment Domestic resource represents one single Domestic payment order entered by the user. SEPA format is already used also for Domestic payment in AT, SK, therefore Domestic and SEPA formats are very similar.
This resource is used for payment orders in domestic local clearing system and for internal bank transfers (between accounts of local bank).

Description of **PaymentDomestic** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------|-----------------|----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order             | structure       | M        | Yes      | Order structure (needed because of signInfo object attached to payment order)                                                                                                                                                      |                                                                                                                                                                                    |
| 2     | id                | TEXT            | M        | No       | Internal identifier of payment order (provided as response after payment creation from BE)                                                                                                                                         |                                                                                                                                                                                    |
| 2     | referenceId       | TEXT            | O        | No       | Transaction reference ID provided by BE when payment order was executed                                                                                                                                                            |                                                                                                                                                                                    |
| 2     | orderCategory     | ENUM            | M        | No       | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [DOMESTIC, OWN_TRANSFER, INTRA_BANK]                                                                                                                               `  |
| 2     | orderType         | ENUM            | M        | No       | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT, DIRECT_DEBIT_IN] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used so far), `DIRECT_DEBIT_IN` (incoming DD from partner's account bank) |
| 2     | state             | ENUM            | M        | No       | State of payment order presented to user on FE, value is mapped based on provided BE technical states.                                                                                                                             | ENUM values: [CREATED, OPEN, SPOOLED, CANCELLED, CLOSED, DELETED]                                                                                                                  |
| 2     | stateDetail       | ENUM            | M        | No       | State detail of payment order provided based on BE technical states. Mapping between technical BE states and predefined FE detail states should be specified by local API. Value is used in FE to display status description.      | ENUM values: [CRE, OPN, INB, TAF, STO, KAG, SNM, NGD, NGA, ADB, AGB, OBG, UNG, BNZ, ENE, TRM, OFL, RPS, CHK, PNR, WER, FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK]      |
| 2     | stateOk           | BOOLEAN         | O        | No       | Indicator whether state (stateDetail value) of payment order is OK from user point of view.                                                                                                                                        | Boolean values: `true`/`false` - For mapping between stateDetail and stateOk indicator values see table in *Payment* type.                                                         |
| 2     | sender            | ACCOUNTNO       | M        | Yes      | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName        | TEXT35          | O        | Yes      | Name of sender, who created payment order (value provided by BE)                                                                                                                                                                   |                                                                                                                                                                                    |
| 2     | senderReference   | TEXT140         | O        | Yes      | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | symbols           | structure       | O        | Yes      | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####. Fields will by parsed from Sender Reference matching corresponding symbols.) |                                                                                                                                                                    |
| 3     | variableSymbol    | TEXT10          | O        | Yes      | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                                                              |                                                                                                                                                                                    |
| 3     | specificSymbol    | TEXT10          | O        | Yes      | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                                                              |                                                                                                                                                                                    |
| 3     | constantSymbol    | TEXT4           | O        | Yes      | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                                                |                                                                                                                                                                                    |
| 2     | receiver          | ACCOUNTNO       | M        | Yes      | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName      | TEXT35          | O        | Yes      | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount            | AMOUNT          | M        | Yes      | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate      | DATE            | O        | Yes      | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | modificationDate  | DATETIME        | O        | No       | Modification date indicates the last update of payment order done by user or BE system (read-only field provided by BE)                                                                                                            | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | executionDate     | DATETIME        | O        | No       | Datetime when payment order was created/updated (the last time) by user (read-only field is automatically setup/changed by BE system based on POST/PUT request)                                                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | expirationDate    | DATETIME        | O        | Yes      | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | repetitionDays    | INTEGER         | O        | Yes      | Number of days after transfer date till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                     | E.g. integer from interval 1-14 in SK                                                                                                                                              |
| 2     | additionalInfo    | structure       | O        | Yes      | Payment Additional info structure with optional elements. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                                    |                                                                                                                                                                                    |
| 3     | text4x35          | ARRAY of TEXT35 | O        | Yes      | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 3     | paymentReference  | TEXT140         | O        | Yes      | Payment reference used to identify payment order on receiver side. (Used only for Domestic/SEPA format)                                                                                                                            |                                                                                                                                                                                    |
| 2     | confirmations     | ARRAY of        | O        | Yes      | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId         | TEXT            | O        | Yes      | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email             | EMAIL           | M        | Yes      | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language          | ENUM            | M        | Yes      | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId         | ENUM            | O        | Yes      | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, E_COMMERCE, UNKNOWN]                                                                  |
| 2     | applicationId     | ENUM            | O        | Yes      | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags             | FLAGS           | O        | Yes      | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |
| 1     | signInfo          | SIGNINFO        | O        | No       | SignInfo Details, consists of state and signId - Hash value calculated using common relevant payment order attributes to ensure the same unchanged object (payment order) is used every time during authorization signing process  |                                                                                                                                                                                    |

The following flags can be applied to field *flags* in **PaymentDomestic** resource in input payload:

Flag         | Description
-------------|-----------------------------------------------
`urgent`     | Flag indicating urgent payment order (in SEPA or local bank clearing systems) requested by client
`collective` | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

The following flags can be applied to field *flags* in **PaymentDomestic** resource in output payload (response):

Flag                       | Description
---------------------------|-----------------------------------------------
`urgent`                   | Flag indicating urgent payment order (in SEPA, SWIFT and maybe also in local bank clearing systems) requested by client
`editable`                 | Flag indicating if payment order can be edited by client
`deletable`                | Flag indicating if payment order can be deleted by client. Already signed payment order must be canceled before deleting.
`cancelable`               | Flag indicating if future dated (already signed) payment order can be canceled by client
`collective`               | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)
`advanceInterestBonusLost` | This flag is returned if signing this order will (probably) result in lost savings. (Specific for AT only)

+ Parameters
    + id (TEXT) ... ID internal identifier of payment order used as part of URI.

+ Model

    + Body

            {
                "order": {
                    "id": "043721778790000001007101",
                    "orderCategory": "DOMESTIC",  // could be also "OWN_TRANSFER" or "INTRA_BANK" in some cases based on local implementation
                    "orderType": "PAYMENT_OUT",
                    "state": "OPEN",
                    "stateDetail": "OPN",
                    "stateOk": true,
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2014-777",
                    "receiver": {
                        "iban": "AT961100000976007260",
                        "bic": "BKAUATWWXXX"
                    },
                    "receiverName": "Maximus Mustermann",
                    "amount": {
                        "value": 3250,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2014-12-19",          
                    "modificationDate": "2014-12-17T23:00:00Z",
                    "executionDate": "2014-12-17T23:00:00Z",
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2014"
                    },
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "d1bc8d3ba4afc7e109612cb"
                }
            }

### Update a one single Domestic payment order [PUT]
Update data for one specific Domestic payment order entered by user based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentDomestic** updated resource, when original data was received by GET payments/{id} call identified by parameter ID.

#### Reply
**PaymentDomestic** resource stored in BE (or Payment Store&Forward local solution) containing updated details of one user payment order.

#### Error codes
Error code                          | Scope             | Purpose
------------------------------------|-------------------|------------------------------------
`ID_NOT_FOUND`                      | id                | The provided ID does not exist.
`ID_MISMATCH`                       | id                | The ID given in the payload does not match the ID given in the URI.
`STATE_INVALID`                     | state             | The order is in a state that does not allow changes.
`DELETED`                           | state             | The order is already deleted.
`CHANGE_INVALID`                    | paymentCategory   | User tried to change the category of order (domestic/sepa/international).
`CATEGORY_INVALID`                  | paymentCategory   | Payment order doesn't belong to defined category (DOMESTIC, OWN_TRANSFER, INTRA_BANK)
`TAC_LIMIT_EXCEEDED`                | amount            | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount            | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount            | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount            | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency          | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference   | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | receiverName      | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | paymentReference  | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | text4x35          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35          | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35          | Too many text items in additionalInfo/text.
`NOT_SUPPORTED_BY_ORDERTYPE`        | aditionalInfo     | PaymentReference is not supported for this orderCategory/Type.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate      | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate      | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate      | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender            | The sender account is offline, therefore no payment orders can be created/updated.
`DOMESTIC_PAYMENTS_NOT_ALLOWED`     | sender            | Order classified as Domestic, but sending account does not have a domesticTransfersAllowed flag.
`OWN_TRANSFERS_NOT_ALLOWED`         | sender            | Order classified as OwnTransfer, but the sending account does not have a ownTransfersAllowed flag.
`ORDER_NOT_POSSIBLE`                | receiver          | Order classified as OwnTransfer, sending account is a profit card or savings account, but the receiver is not an own account.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`RECEIVER_NOT_ALLOWED`              | receiver          | The receiver account is not between predefined available accounts for this sender account, so the transfer is not allowed.
`DOMESTIC_PAYMENTS_DISABLED`        | channelId         | Currently no domestic payments are possible at all (server setting).

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "id": "043721778790000001007101",
                    "orderCategory": "DOMESTIC",  // could be also "OWN_TRANSFER" or "INTRA_BANK" in some cases based on local implementation
                    "orderType": "PAYMENT_OUT",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2014-777",
                    "receiver": {
                        "iban": "AT961100000976007260",
                        "bic": "BKAUATWWXXX"
                    },
                    "receiverName": "Maximus Mustermann",
                    "amount": {
                        "value": 3250,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2014-12-19",          
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2014"
                    },
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                }
            }

+ Response 200 (application/json)

    [PaymentDomestic][]


## PaymentDomesticEntry [/netbanking/my/orders/payments/domestic]
Payment Domestic Entry resource represents creation of new single Domestic payment order entered by the user. Resource uses subset of attributes of embedded **PaymentDomestic** resource.

Description of **PaymentDomesticEntry** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------|-----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order             | structure       | M        | Order structure (legacy of PRESTO)                                                                                                                                                                                                 |                                                                                                                                                                                    |
| 2     | orderCategory     | ENUM            | M        | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [DOMESTIC, OWN_TRANSFER, INTRA_BANK]                                                                                                                                  |
| 2     | orderType         | ENUM            | M        | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used so far)                                                                 |
| 2     | sender            | ACCOUNTNO       | M        | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName        | TEXT35          | O        | Name of sender, who created payment order                                                                                                                                                                                          |                                                                                                                                                                                    |
| 2     | senderReference   | TEXT140         | O        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | symbols           | structure       | O        | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####. Fields will by parsed from Sender Reference matching corresponding symbols.) |                                                                                                                                                                    |
| 3     | variableSymbol    | TEXT10          | O        | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                                                              |                                                                                                                                                                                    |
| 3     | specificSymbol    | TEXT10          | O        | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                                                              |                                                                                                                                                                                    |
| 3     | constantSymbol    | TEXT4           | O        | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                                                |                                                                                                                                                                                    |
| 2     | receiver          | ACCOUNTNO       | M        | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName      | TEXT35          | O        | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount            | AMOUNT          | M        | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate      | DATE            | O        | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | expirationDate    | DATETIME        | O        | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | repetitionDays    | INTEGER         | O        | Number of days after transfer date till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                     | E.g. integer from interval 1-14 in SK                                                                                                                                              |
| 2     | additionalInfo    | One Of          | O        | Payment Additional info structure with exclusive elements - Only One Of them could be used in the payment order. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                             |                                                                                                                                                                                    |
| 3     | text4x35          | ARRAY of TEXT35 | C        | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 3     | paymentReference  | TEXT140         | C        | Payment reference used to identify payment order on receiver side. (Used only for SEPA format)                                                                                                                                     |                                                                                                                                                                                    |
| 2     | confirmations     | ARRAY of        | O        | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId         | TEXT            | O        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email             | EMAIL           | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language          | ENUM            | M        | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId         | ENUM            | O        | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH,E_COMMERCE, UNKNOWN]                                                                   |
| 2     | applicationId     | ENUM            | O        | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags             | FLAGS           | O        | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |

The following flags can be applied to field *flags* in **PaymentDomesticEntry** resource:

Flag         | Description
-------------|-----------------------------------------------
`urgent`     | Flag indicating urgent payment order (in SEPA or in local bank clearing systems) requested by client
`collective` | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "orderCategory": "DOMESTIC",  // could be also "OWN_TRANSFER" or "INTRA_BANK" in some cases based on local implementation
                    "orderType": "PAYMENT_OUT",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2014-777",
                    "receiver": {
                        "iban": "AT961100000976007260",
                        "bic": "BKAUATWWXXX"
                    },
                    "receiverName": "Maximus Mustermann",
                    "amount": {
                        "value": 3250,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2014-12-19",          
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2014"
                    },
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                }
            }

### Create a one single Domestic payment order [POST]
Create one new specific Domestic payment order entered by user. In case the call fails with a serverside field check, the check should continue as far as possible and as many errors as possible should be detected at one POST. However, there is no guarantee that the API delivers all wrong fields at once for one POST call.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentDomesticEntry** resource with all required data entered by user in FE application.

#### Reply
**PaymentDomestic** resource stored in BE (or Payment Store&Forward local solution) containing details of one user payment order.

#### Error codes
If user is not allowed to create this payment order, e.g. because the account does not allow domestic transfers (see (ACCOUNT) for according flags), a HTTP 403 is returned, and the error code explains why:

Error code                          | Scope             | Purpose
------------------------------------|-------------------|------------------------------------
`CATEGORY_INVALID`                  | paymentCategory   | Payment order doesn't belong to defined category (DOMESTIC, OWN_TRANSFER, INTRA_BANK)
`TAC_LIMIT_EXCEEDED`                | amount            | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount            | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount            | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount            | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency          | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference   | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | receiverName      | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | paymentReference  | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | text4x35          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35          | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35          | Too many text items in additionalInfo/text.
`NOT_SUPPORTED_BY_ORDERTYPE`        | aditionalInfo     | PaymentReference is not supported for this orderCategory/Type.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate      | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate      | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate      | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender            | The sender account is offline, therefore no payment orders can be created/updated.
`DOMESTIC_PAYMENTS_NOT_ALLOWED`     | sender            | Order classified as Domestic, but sending account does not have a domesticTransfersAllowed flag.
`OWN_TRANSFERS_NOT_ALLOWED`         | sender            | Order classified as OwnTransfer, but the sending account does not have a ownTransfersAllowed flag.
`ORDER_NOT_POSSIBLE`                | receiver          | Order classified as OwnTransfer, sending account is a profit card or savings account, but the receiver is not an own account.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`RECEIVER_NOT_ALLOWED`              | receiver          | The receiver account is not between predefined available accounts for this sender account, so the transfer is not allowed.
`DOMESTIC_PAYMENTS_DISABLED`        | channelId         | Currently no domestic payments are possible at all (server setting).

+ Request

    [PaymentDomesticEntry][]

+ Response 200 (application/json)

    [PaymentDomestic][]


## PaymentSepa [/netbanking/my/orders/payments/sepa/{id}]
Payment SEPA resource represents one single SEPA payment order entered by the user. SEPA format is already used also for Domestic payment in AT, SK, therefore Domestic and SEPA format are very similar.
This resource should be used for payment orders only in EUR currency outside the local country to country which belongs to Single Euro Payments Area (SEPA).

Description of **PaymentSepa** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------|-----------------|----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order             | structure       | M        | Yes      | Order structure (needed because of signInfo object attached to payment order)                                                                                                                                                      |                                                                                                                                                                                    |
| 2     | id                | TEXT            | M        | No       | Internal identifier of payment order (provided as response after payment creation from BE)                                                                                                                                         |                                                                                                                                                                                    |
| 2     | referenceId       | TEXT            | O        | No       | Transaction reference ID provided by BE when payment order was executed                                                                                                                                                            |                                                                                                                                                                                    |
| 2     | orderCategory     | ENUM            | M        | No       | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [SEPA]                                                                                                                                                                |
| 2     | orderType         | ENUM            | M        | No       | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used so far)                                                                 |
| 2     | state             | ENUM            | M        | No       | State of payment order presented to user on FE, value is mapped based on provided BE technical states.                                                                                                                             | ENUM values: [CREATED, OPEN, SPOOLED, CANCELLED, CLOSED, DELETED]                                                                                                                  |
| 2     | stateDetail       | ENUM            | M        | No       | State detail of payment order provided based on BE technical states. Mapping between technical BE states and predefined FE detail states should be specified by local API. Value is used in FE to display status description.      | ENUM values: [CRE, OPN, INB, TAF, STO, KAG, SNM, NGD, NGA, ADB, AGB, OBG, UNG, BNZ, ENE, TRM, OFL, RPS, CHK, PNR, WER, FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK]      |
| 2     | stateOk           | BOOLEAN         | O        | No       | Indicator whether state (stateDetail value) of payment order is OK from user point of view.                                                                                                                                        | Boolean values: `true`/`false` - For mapping between stateDetail and stateOk indicator values see table in *Payment* type.                                                         |
| 2     | sender            | ACCOUNTNO       | M        | Yes      | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName        | TEXT35          | O        | Yes      | Name of sender, who created payment order (value provided by BE)                                                                                                                                                                   |                                                                                                                                                                                    |
| 2     | senderReference   | TEXT140         | O        | Yes      | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | receiver          | ACCOUNTNO       | M        | Yes      | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName      | TEXT35          | M        | Yes      | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount            | AMOUNT          | M        | Yes      | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate      | DATE            | M        | Yes      | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | modificationDate  | DATETIME        | O        | No       | Modification date indicates the last update of payment order done by user or BE system (read-only field provided by BE)                                                                                                            | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | executionDate     | DATETIME        | O        | No       | Datetime when payment order was created/updated (the last time) by user (read-only field is automatically setup/changed by BE system based on POST/PUT request)                                                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | expirationDate    | DATETIME        | O        | Yes      | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | repetitionDays    | INTEGER         | O        | Yes      | Number of days after transfer date till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                     | E.g. integer from interval 1-14 in SK                                                                                                                                              |
| 2     | additionalInfo    | structure       | O        | Yes      | Payment Additional info structure with optional elements. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                                    |                                                                                                                                                                                    |
| 3     | text4x35          | ARRAY of TEXT35 | O        | Yes      | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 3     | paymentReference  | TEXT140         | O        | Yes      | Payment reference used to identify payment order on receiver side. (Used only for SEPA format)                                                                                                                                     |                                                                                                                                                                                    |
| 2     | feeSharingCode    | ENUM            | O        | No       | Transfer Fee sharing code, mandatory for International payment, default value `SHA` for SEPA payment, not used for Domestic payment.                                                                                               | ENUM values: [SHA] default value for SEPA is `SHA` - Transfer fee is shared by both                                                                                                |
| 2     | senderAccountName | TEXT            | O        | No       | Name of sender account, payment order is transferred from (value provided by BE) (Used only for SEPA and International payments)                                                                                                   |                                                                                                                                                                                    |
| 2     | senderAddress     | ARRAY of TEXT   | O        | Yes      | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Possible for SEPA payments)                                         | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverAddress   | ARRAY of TEXT   | O        | No       | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Possible for SEPA payments, but not used in George)               | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | confirmations     | ARRAY of        | O        | Yes      | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId         | TEXT            | O        | Yes      | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email             | EMAIL           | M        | Yes      | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language          | ENUM            | M        | Yes      | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId         | ENUM            | O        | Yes      | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, E_COMMERCE, UNKNOWN]                                                                  |
| 2     | applicationId     | ENUM            | O        | Yes      | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags             | FLAGS           | O        | Yes      | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |
| 1     | signInfo          | SIGNINFO        | O        | No       | SignInfo Details, consists of state and signId - Hash value calculated using common relevant payment order attributes to ensure the same unchanged object (payment order) is used every time during authorization signing process  |                                                                                                                                                                                    |

The following flags can be applied to field *flags* in **PaymentSepa** resource in input payload:

Flag         | Description
-------------|-----------------------------------------------
`urgent`     | Flag indicating urgent payment order (in SEPA or local bank clearing systems) requested by client
`collective` | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

The following flags can be applied to field *flags* in **PaymentSepa** resource in output payload (response):

Flag                       | Description
---------------------------|-----------------------------------------------
`urgent`                   | Flag indicating urgent payment order (in SEPA, SWIFT and maybe also in local bank clearing systems) requested by client
`editable`                 | Flag indicating if payment order can be edited by client
`deletable`                | Flag indicating if payment order can be deleted by client. Already signed payment order must be canceled before deleting.
`cancelable`               | Flag indicating if future dated (already signed) payment order can be canceled by client
`collective`               | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)
`advanceInterestBonusLost` | This flag is returned if signing this order will (probably) result in lost savings. (Specific for AT only)

+ Parameters
    + id (TEXT) ... ID internal identifier of payment order used as part of URI.

+ Model

    + Body

            {
                "order": {
                    "id": "04372177879000000145353",
                    "orderCategory": "SEPA",
                    "orderType": "PAYMENT_OUT",
                    "state": "OPEN",
                    "stateDetail": "OPN",
                    "stateOk": true,
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2015-777",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 3250,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2015-12-19",          
                    "modificationDate": "2015-12-17T23:00:00Z",
                    "executionDate": "2015-12-17T23:00:00Z",
                    "additionalInfo": {
                        "paymentReference": "PayRef 754-2015"
                    },
                    "feeSharingCode": "SHA",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "d1bc8d3ba4afc7e109612cb"
                }
            }

### Update a one single SEPA payment order [PUT]
Update data for one specific SEPA payment order entered by user based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentSepa** updated resource, when original data was received by GET payments/{id} call identified by parameter ID.

#### Reply
**PaymentSepa** resource stored in BE (or Payment Store&Forward local solution) containing updated details of one user payment order.

#### Error codes
Error code                          | Scope             | Purpose
------------------------------------|-------------------|------------------------------------
`ID_NOT_FOUND`                      | id                | The provided ID does not exist.
`ID_MISMATCH`                       | id                | The ID given in the payload does not match the ID given in the URI.
`STATE_INVALID`                     | state             | The order is in a state that does not allow changes.
`DELETED`                           | state             | The order is already deleted.
`CHANGE_INVALID`                    | paymentCategory   | User tried to change the category of order (domestic/sepa/international).
`CATEGORY_INVALID`                  | paymentCategory   | Payment order doesn't belong to defined category (SEPA)
`TAC_LIMIT_EXCEEDED`                | amount            | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount            | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount            | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount            | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency          | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference   | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | senderAddress     | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverName      | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverAddress   | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | paymentReference  | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | text4x35          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35          | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35          | Too many text items in additionalInfo/text.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate      | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate      | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate      | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender            | The sender account is offline, therefore no payment orders can be created/updated.
`ORDER_NOT_POSSIBLE`                | sender            | Order classified as SEPA, but sending account does not have a internationalTransfersAllowed flag.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`RECEIVER_NOT_IBAN`                 | receiver          | Receiver account is provided in BBAN format (instead of IBAN) and countryCode refers to a SEPA country and payment currency is EUR.
`INTERNATIONAL_PAYMENTS_DISABLED`   | channelId         | Currently no international payments are possible at all (server setting).

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "id": "04372177879000000145353",
                    "orderCategory": "SEPA",
                    "orderType": "PAYMENT_OUT",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2014-777",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 30050,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2015-12-19",          
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2014"
                    },
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                }
            }

+ Response 200 (application/json)

    [PaymentSepa][]


## PaymentSepaEntry [/netbanking/my/orders/payments/sepa]
Payment SEPA Entry resource represents creation of new single SEPA payment order entered by the user. Resource uses subset of attributes of embedded **PaymentSepa** resource.

Description of **PaymentSepaEntry** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------|-----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order             | structure       | M        | Order structure (legacy of PRESTO)                                                                                                                                                                                                 |                                                                                                                                                                                    |
| 2     | orderCategory     | ENUM            | M        | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [SEPA]                                                                                                                                                                |
| 2     | orderType         | ENUM            | M        | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT, DIRECT_DEBIT_OUT] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used so far)                                                                 |
| 2     | sender            | ACCOUNTNO       | M        | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName        | TEXT35          | O        | Name of sender, who created payment order                                                                                                                                                                                          |                                                                                                                                                                                    |
| 2     | senderReference   | TEXT140         | O        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | receiver          | ACCOUNTNO       | M        | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName      | TEXT35          | M        | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount            | AMOUNT          | M        | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate      | DATE            | M        | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | expirationDate    | DATETIME        | O        | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | repetitionDays    | INTEGER         | O        | Number of days after transfer date till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                     | E.g. integer from interval 1-14 in SK                                                                                                                                              |
| 2     | additionalInfo    | One Of          | O        | Payment Additional info structure with exclusive elements - Only One Of them could be used in the payment order. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                             |                                                                                                                                                                                    |
| 3     | text4x35          | ARRAY of TEXT35 | C        | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 3     | paymentReference  | TEXT140         | C        | Payment reference used to identify payment order on receiver side. (Used only for SEPA format)                                                                                                                                     |                                                                                                                                                                                    |
| 2     | feeSharingCode    | ENUM            | O        | Transfer Fee sharing code, mandatory for International payment, default value `SHA` for SEPA payment, not used for Domestic payment.                                                                                               | ENUM values: [SHA] default value for SEPA is `SHA` - Transfer fee is shared by both                                                                                                |
| 2     | senderAddress     | ARRAY of TEXT   | O        | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Possible for SEPA payments)                                         | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverAddress   | ARRAY of TEXT   | O        | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Possible for SEPA payments, but not used in George)               | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | confirmations     | ARRAY of        | O        | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId         | TEXT            | O        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email             | EMAIL           | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language          | ENUM            | M        | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId         | ENUM            | O        | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH,E_COMMERCE, UNKNOWN]                                                                   |
| 2     | applicationId     | ENUM            | O        | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags             | FLAGS           | O        | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |

The following flags can be applied to field *flags* in **PaymentSepaEntry** resource:

Flag         | Description
-------------|-----------------------------------------------
`urgent`     | Flag indicating urgent payment order (in SEPA or in local bank clearing systems) requested by client
`collective` | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "orderCategory": "SEPA",
                    "orderType": "PAYMENT_OUT",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "MY INV-2014-777",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 12350,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "transferDate": "2015-10-19",          
                    "additionalInfo": {
                        "paymentReference": "PayRef 754786-2015"
                    },
                    "feeSharingCode": "SHA",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                }
            }

### Create a one single SEPA payment order [POST]
Create one new specific SEPA payment order entered by user. In case the call fails with a serverside field check, the check should continue as far as possible and as many errors as possible should be detected at one POST. However, there is no guarantee that the API delivers all wrong fields at once for one POST call.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentSepaEntry** resource with all required data entered by user in FE application.

#### Reply
**PaymentSepa** resource stored in BE (or Payment Store&Forward local solution) containing details of one user payment order.

#### Error codes
If user is not allowed to create this payment order, e.g. because the account does not allow international transfers (see (ACCOUNT) for according flags), a HTTP 403 is returned, and the error code explains why:

Error code                          | Scope             | Purpose
------------------------------------|-------------------|------------------------------------
`CATEGORY_INVALID`                  | paymentCategory   | Payment order doesn't belong to defined category (SEPA)
`TAC_LIMIT_EXCEEDED`                | amount            | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount            | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount            | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount            | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency          | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference   | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | senderAddress     | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverName      | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverAddress   | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | paymentReference  | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | text4x35          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35          | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35          | Too many text items in additionalInfo/text.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate      | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate      | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate      | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender            | The sender account is offline, therefore no payment orders can be created/updated.
`ORDER_NOT_POSSIBLE`                | sender            | Order classified as SEPA, but sending account does not have a internationalTransfersAllowed flag.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`RECEIVER_NOT_IBAN`                 | receiver          | Receiver account is provided in BBAN format (instead of IBAN) and countryCode refers to a SEPA country and payment currency is EUR.
`INTERNATIONAL_PAYMENTS_DISABLED`   | channelId         | Currently no international payments are possible at all (server setting).

+ Request

    [PaymentSepaEntry][]

+ Response 200 (application/json)

    [PaymentSepa][]


## PaymentInternational [/netbanking/my/orders/payments/international/{id}]
Payment International resource represents one single international (SWIFT) payment order entered by the user.
This resource should be used for payment orders outside the local bank in other then local currency or when Domestic and SEPA payment may not be used.

Description of **PaymentInternational** resource attributes: 

| Level | Attribute name      | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|---------------------|-----------------|----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order               | structure       | M        | Yes      | Order structure (needed because of signInfo object attached to payment order)                                                                                                                                                      |                                                                                                                                                                                    |
| 2     | id                  | TEXT            | M        | No       | Internal identifier of payment order (provided as response after payment creation from BE)                                                                                                                                         |                                                                                                                                                                                    |
| 2     | referenceId         | TEXT            | O        | No       | Transaction reference ID provided by BE when payment order was executed                                                                                                                                                            |                                                                                                                                                                                    |
| 2     | orderCategory       | ENUM            | M        | No       | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [INTERNATIONAL]                                                                                                                                                       |
| 2     | orderType           | ENUM            | M        | No       | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used for SWIFT)                                                                                |
| 2     | state               | ENUM            | M        | No       | State of payment order presented to user on FE, value is mapped based on provided BE technical states.                                                                                                                             | ENUM values: [CREATED, OPEN, SPOOLED, CANCELLED, CLOSED, DELETED]                                                                                                                  |
| 2     | stateDetail         | ENUM            | M        | No       | State detail of payment order provided based on BE technical states. Mapping between technical BE states and predefined FE detail states should be specified by local API. Value is used in FE to display status description.      | ENUM values: [CRE, OPN, INB, TAF, STO, KAG, SNM, NGD, NGA, ADB, AGB, OBG, UNG, BNZ, ENE, TRM, OFL, RPS, CHK, PNR, WER, FIN, FIH, FIM, FIX, FIR, FIK, FID, BLA, FUS, ABG, UNK]      |
| 2     | stateOk             | BOOLEAN         | O        | No       | Indicator whether state (stateDetail value) of payment order is OK from user point of view.                                                                                                                                        | Boolean values: `true`/`false` - For mapping between stateDetail and stateOk indicator values see table in *Payment* type.                                                         |
| 2     | sender              | ACCOUNTNO       | M        | Yes      | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName          | TEXT35          | O        | Yes      | Name of sender, who created payment order (value provided by BE)                                                                                                                                                                   |                                                                                                                                                                                    |
| 2     | senderReference     | TEXT140         | O        | Yes      | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | receiver            | ACCOUNTNO       | M        | Yes      | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName        | TEXT35          | M        | Yes      | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount              | AMOUNT          | M        | Yes      | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate        | DATE            | M        | Yes      | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | modificationDate    | DATETIME        | O        | No       | Modification date indicates the last update of payment order done by user or BE system (read-only field provided by BE)                                                                                                            | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | executionDate       | DATETIME        | O        | No       | Datetime when payment order was created/updated (the last time) by user (read-only field is automatically setup/changed by BE system based on POST/PUT request)                                                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | expirationDate      | DATETIME        | O        | Yes      | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | additionalInfo      | structure       | O        | Yes      | Payment Additional info structure with optional elements. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                                    |                                                                                                                                                                                    |
| 3     | text4x35            | ARRAY of TEXT35 | O        | Yes      | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 2     | feeSharingCode      | ENUM            | O        | Yes      | Transfer Fee sharing code, mandatory for International payment, default value `SHA` for SEPA payment, not used for Domestic payment.                                                                                               | ENUM values: [OUR, SHA, BEN] `OUR` - Transfer fee paid by sender, `SHA` - Transfer fee is shared by both, `BEN` - Transfer fee paid by receiver/beneficiary.                       |
| 2     | senderAccountName   | TEXT            | O        | No       | Name of sender account, payment order is transferred from (value provided by BE) (Used only for SEPA and International payments)                                                                                                   |                                                                                                                                                                                    |
| 2     | senderAddress       | ARRAY of TEXT   | O        | Yes      | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name.                                                                      | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverAddress     | ARRAY of TEXT   | O        | Yes      | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name.                                                                    | array of 3x string with max lenght 35 characters each                                                                                                                              |
| 2     | receiverBankName    | TEXT            | O        | Yes      | Receiver's Bank name of receiver's account, where payment order is transferred to (value provided by BE) (Used only for International payments)                                                                                    |                                                                                                                                                                                    |
| 2     | receiverBankAddress | ARRAY of TEXT   | O        | Yes      | Receiver's Bank address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International payments)                        | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | interbank           | BIC             | O        | Yes      | BIC of in-between (correspondent) bank if used in International transfer. (Used only for International payments)                                                                                                                   |                                                                                                                                                                                    |
| 2     | confirmations       | ARRAY of        | O        | Yes      | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId           | TEXT            | O        | Yes      | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email               | EMAIL           | M        | Yes      | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language            | ENUM            | M        | Yes      | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId           | ENUM            | O        | Yes      | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, E_COMMERCE, UNKNOWN]                                                                  |
| 2     | applicationId       | ENUM            | O        | Yes      | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags               | FLAGS           | O        | Yes      | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |
| 1     | signInfo            | SIGNINFO        | O        | No       | SignInfo Details, consists of state and signId - Hash value calculated using common relevant payment order attributes to ensure the same unchanged object (payment order) is used every time during authorization signing process  |                                                                                                                                                                                    |

The following flags can be applied to field *flags* in **PaymentInternational** resource in input payload:

Flag                    | Description
------------------------|-----------------------------------------------
`urgent`                | Flag indicating urgent payment order requested by client
`individualConditions`  | Flag indicating individual conditions for international payment order requested by client
`collective`            | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

The following flags can be applied to field *flags* in **PaymentInternational** resource in output payload (response):

Flag                       | Description
---------------------------|-----------------------------------------------
`urgent`                   | Flag indicating urgent payment order (in SEPA, SWIFT and maybe also in local bank clearing systems) requested by client
`editable`                 | Flag indicating if payment order can be edited by client
`deletable`                | Flag indicating if payment order can be deleted by client. Already signed payment order must be canceled before deleting.
`cancelable`               | Flag indicating if future dated (already signed) payment order can be canceled by client
`individualConditions`     | Flag indicating individual conditions for international payment order requested by client
`collective`               | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

+ Parameters
    + id (TEXT) ... ID internal identifier of payment order used as part of URI.

+ Model

    + Body

            {
                "order": {
                    "id": "INT0437217788686353",
                    "orderCategory": "INTERNATIONAL",
                    "orderType": "PAYMENT_OUT",
                    "state": "OPEN",
                    "stateDetail": "OPN",
                    "stateOk": true,
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "SWIFT INV-2015-666",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 3001250,
                        "precision": 2,
                        "currency": "USD"
                    },
                    "transferDate": "2015-12-19",          
                    "modificationDate": "2015-12-17T23:00:00Z",
                    "executionDate": "2015-12-17T23:00:00Z",
                    "additionalInfo": {
                        "text4x35": [
                            "Payment ID 8989-2015"
                        ]
                    },
                    "feeSharingCode": "OUR",
                    "senderAccountName": "Manfred Dichtl",
                    "receiverAddress": [
                        "Hlavni 12",
                        "141 00 Praha 4",
                        "Czech Republic"
                    ],
                    "receiverBankName": "Ceska sporitelna",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "flags": [
                        "individualConditions"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "lcjnsdcafc7e109612cb"
                }
            }

### Update a one single International payment order [PUT]
Update data for one specific International (SWIFT) payment order entered by user based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentInternational** updated resource, when original data was received by GET payments/{id} call identified by parameter ID.

#### Reply
**PaymentInternational** resource stored in BE (or Payment Store&Forward local solution) containing updated details of one user payment order.

#### Error codes
Error code                          | Scope               | Purpose
------------------------------------|---------------------|------------------------------------
`ID_NOT_FOUND`                      | id                  | The provided ID does not exist.
`ID_MISMATCH`                       | id                  | The ID given in the payload does not match the ID given in the URI.
`STATE_INVALID`                     | state               | The order is in a state that does not allow changes.
`DELETED`                           | state               | The order is already deleted.
`CHANGE_INVALID`                    | paymentCategory     | User tried to change the category of order (domestic/sepa/international).
`CATEGORY_INVALID`                  | paymentCategory     | Payment order doesn't belong to defined category (INTERNATIONAL)
`TAC_LIMIT_EXCEEDED`                | amount              | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount              | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount              | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount              | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency            | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference     | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | senderAddress       | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverAddress     | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverBankName    | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverBankAddress | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | text4x35            | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35            | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35            | Too many text items in additionalInfo/text.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate        | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate        | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate        | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender              | The sender account is offline, therefore no payment orders can be created/updated.
`ORDER_NOT_POSSIBLE`                | sender              | Order classified as International, but sending account does not have a internationalTransfersAllowed flag.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`COMBINATION_INVALID`               | receiver            | The account number of the receiver is indicated in an invalid way (e.g. IBAN and BBAN number are indicated and don't match or IBAN, BIC combination is wrong).
`COUNTRY_INVALID`                   | receiver            | The IBAN or BIC of the receiver refers to a country where no international transfers are possible at the moment.
`INTERBANK_NOT_FOUND`               | interbank           | Provided interbank BIC could not be found.
`INTERNATIONAL_PAYMENTS_DISABLED`   | channelId           | Currently no international payments are possible at all (server setting).

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "id": "INT0437217788686353",
                    "orderCategory": "INTERNATIONAL",
                    "orderType": "PAYMENT_OUT",
                    "state": "OPEN",
                    "stateDetail": "OPN",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderName": "Manfred Dichtl",
                    "senderReference": "SWIFT INV-2015-666",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 3001250,
                        "precision": 2,
                        "currency": "USD"
                    },
                    "transferDate": "2015-12-19",          
                    "additionalInfo": {
                        "text4x35": [
                            "Payment ID 8989-2015",
                            "More detail info about payment"
                        ]
                    },
                    "feeSharingCode": "OUR",
                    "senderAccountName": "Manfred Dichtl",
                    "receiverAddress": [
                        "Hlavni 12",
                        "141 00 Praha 4",
                        "Czech Republic"
                    ],
                    "receiverBankName": "Ceska sporitelna",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "flags": [
                        "individualConditions"
                    ]
                }
            }

+ Response 200 (application/json)

    [PaymentInternational][]


## PaymentInternationalEntry [/netbanking/my/orders/payments/international]
Payment International Entry resource represents creation of new single SWIFT payment order entered by the user. Resource uses subset of attributes of embedded **PaymentInternational** resource.

Description of **PaymentInternationalEntry** resource attributes: 

| Level | Attribute name      | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|---------------------|-----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | order               | structure       | M        | Order structure (legacy of PRESTO)                                                                                                                                                                                                 |                                                                                                                                                                                    |
| 2     | orderCategory       | ENUM            | M        | Payment order category determines whether payment is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user (domestic, but with better fee policy).   | ENUM values: [INTERNATIONAL]                                                                                                                                                       |
| 2     | orderType           | ENUM            | M        | Payment order type (outgoing payment, outgoing direct debit, incoming direct debit) determines further transaction processing in BE.                                                                                               | ENUM values: [PAYMENT_OUT] - `PAYMENT_OUT` (default value), `DIRECT_DEBIT_OUT` (not used for SWIFT)                                                                                |
| 2     | sender              | ACCOUNTNO       | M        | Account number  of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                                             | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 2     | senderName          | TEXT35          | O        | Name of sender, who created payment order                                                                                                                                                                                          |                                                                                                                                                                                    |
| 2     | senderReference     | TEXT140         | O        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                                                   |                                                                                                                                                                                    |
| 2     | receiver            | ACCOUNTNO       | M        | Account number  of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)                                 |                                                                                                                                                                                    |
| 2     | receiverName        | TEXT35          | M        | Name of receiver of payment order                                                                                                                                                                                                  |                                                                                                                                                                                    |
| 2     | amount              | AMOUNT          | M        | Payment amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                                                                   |                                                                                                                                                                                    |
| 2     | transferDate        | DATE            | M        | Requested due date entered by user (could be in near future), date when payment order should be transferred from user account.                                                                                                     | Default value could be current business day in line with local CutOff times for different types of payment order (domestic, SEPA, SWIFT, intra bank). ISO date format:  YYYY-MM-DD |
| 2     | expirationDate      | DATETIME        | O        | Datetime till when payment order will be repeated on BE in the case of insufficient funds on account                                                                                                                               | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                                                                                                         |
| 2     | additionalInfo      | structure       | O        | Payment Additional info structure. This attribute corresponds to SEPA/SWIFT field remittanceInformation.                                                                                                                           |                                                                                                                                                                                    |
| 3     | text4x35            | ARRAY of TEXT35 | O        | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                                                  |                                                                                                                                                                                    |
| 2     | feeSharingCode      | ENUM            | O        | Transfer Fee sharing code, mandatory for International payment, default value `SHA` for SEPA payment, not used for Domestic payment.                                                                                               | ENUM values: [OUR, SHA, BEN] `OUR` - Transfer fee paid by sender, `SHA` - Transfer fee is shared by both, `BEN` - Transfer fee paid by receiver/beneficiary.                       |
| 2     | senderAddress       | ARRAY of TEXT   | O        | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name.                                                                      | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | receiverAddress     | ARRAY of TEXT   | O        | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name.                                                                    | array of 3x string with max lenght 35 characters each                                                                                                                              |
| 2     | receiverBankName    | TEXT            | O        | Receiver's Bank name of receiver's account, where payment order is transferred to (value provided by BE) (Used only for International payments)                                                                                    |                                                                                                                                                                                    |
| 2     | receiverBankAddress | ARRAY of TEXT   | O        | Receiver's Bank address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International payments)                        | array of 3x string with max length 35 characters each                                                                                                                              |
| 2     | interbank           | BIC             | O        | BIC of in-between (correspondent) bank if used in International transfer. (Used only for International payments)                                                                                                                   |                                                                                                                                                                                    |
| 2     | confirmations       | ARRAY of        | O        | Confirmation structure (possible collection), where user requested confirmation of payment order execution will be sent                                                                                                            |                                                                                                                                                                                    |
| 3     | contactId           | TEXT            | O        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                                                                                                                    |                                                                                                                                                                                    |
| 3     | email               | EMAIL           | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                                                                                                                                | E.g. "john.doe@test.com"                                                                                                                                                           |
| 3     | language            | ENUM            | M        | Predefined language which should be used for confirmation template.                                                                                                                                                                | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu]                                                                                                                            |
| 2     | channelId           | ENUM            | O        | ID of the channel via which this payment order was entered/modified the last time. (This channel ID could be used for filtering in payment list in future)                                                                         | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH,E_COMMERCE, UNKNOWN]                                                                   |
| 2     | applicationId       | ENUM            | O        | ID of the application via which this payment order was entered/modified the last time. (This application ID could be used for filtering in payment list in future)                                                                 | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, GSM, BUSINESS24, TELEPHONE_BANKER, IVR, VIDEO_BANKER, BRANCH_FE, E_PAYMENT, DONATION, ATM_PAYMENT, UNKNOWN]                     |
| 2     | flags               | FLAGS           | O        | Array of optional Flag values depends on Payment order category, type                                                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |

The following flags can be applied to field *flags* in **PaymentInternationalEntry** resource:

Flag                    | Description
------------------------|-----------------------------------------------
`urgent`                | Flag indicating urgent payment order requested by client
`individualConditions`  | Flag indicating individual conditions for international payment order requested by client
`collective`            | This order was created as collaborative order. Orders flagged as collective are not signable (co-signing process must be defined)

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "order": {
                    "orderCategory": "INTERNATIONAL",
                    "orderType": "PAYMENT_OUT",
                    "sender": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "senderReference": "SWIFT INV-2015-888",
                    "receiver": {
                        "iban": "CZ6508000000192000145399",
                        "bic": "GIBACZPX"
                    },
                    "receiverName": "Jara Cimermann",
                    "amount": {
                        "value": 123450,
                        "precision": 2,
                        "currency": "CZK"
                    },
                    "transferDate": "2015-12-19",          
                    "additionalInfo": {
                        "text4x35": [
                            "Payment ID 954395-2015",
                            "More detail info about payment"
                        ]
                    },
                    "feeSharingCode": "SHA",
                    "receiverBankName": "Ceska sporitelna",
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE"
                }
            }

### Create a one single International payment order [POST]
Create one new specific International SWIFT payment order entered by user. In case the call fails with a serverside field check, the check should continue as far as possible and as many errors as possible should be detected at one POST. However, there is no guarantee that the API delivers all wrong fields at once for one POST call.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**PaymentInternationalEntry** resource with all required data entered by user in FE application.

#### Reply
**PaymentInternational** resource stored in BE (or Payment Store&Forward local solution) containing details of one user payment order.

#### Error codes
If user is not allowed to create this payment order, e.g. because the account does not allow international transfers (see (ACCOUNT) for according flags), a HTTP 403 is returned, and the error code explains why:

Error code                          | Scope               | Purpose
------------------------------------|---------------------|------------------------------------
`CATEGORY_INVALID`                  | paymentCategory     | Payment order doesn't belong to defined category (INTERNATIONAL)
`TAC_LIMIT_EXCEEDED`                | amount              | TAC limit is exceeded.
`TAN_LIMIT_EXCEEDED`                | amount              | TAN limit is exceeded.
`ACCOUNT_MANAGER_NEEDED`            | amount              | The order has to be cleared by an account manager of the bank.
`VALUE_INVALID`                     | amount              | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`CURRENCY_NOT_AVAILABLE`            | currency            | The requested payment currency is not available (for whatever reason).
`FIELD_INVALID`                     | senderName          | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | senderReference     | Field does not match local regular expression (e.g. for AT: `/^[\w \/-?:\(\)\.,\'+]+$/`)
`FIELD_INVALID`                     | senderAddress       | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverName        | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverAddress     | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverBankName    | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | receiverBankAddress | Field has more than 3 lines, lines longer than 35 or lines do not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`)
`FIELD_INVALID`                     | text4x35            | Field does not match local regular expression (e.g. for AT: `/^[\w öäüÖÄÜß\(\)&\.,-\/+<>?;!""={}:_%*#§|\\ $\^\[\]\'\u20AC@]+$/`) in all lines.
`FIELD_TOO_LONG`                    | text4x35            | The fields contains too many characters for the given format.
`TOO_MUCH_POSITIONS`                | text4x35            | Too many text items in additionalInfo/text.
`FIELD_INVALID_TOO_FAR_IN_FUTURE`   | transferDate        | Date is >90 days in the future (should be local parameter).
`INVALID_DATE_IN_PAST`              | transferDate        | Date is in the past.
`INVALID_DATE_TIME`                 | transferDate        | Time is not set to midnight (UTC) (DATE format should be used).
`ACCOUNT_OFFLINE`                   | sender              | The sender account is offline, therefore no payment orders can be created/updated.
`ORDER_NOT_POSSIBLE`                | sender              | Order classified as International, but sending account does not have a internationalTransfersAllowed flag.
`RECEIVER_BANK_NOT_FOUND`           | receiver/bic or bankCode | Receiver bank is not found.
`COMBINATION_INVALID`               | receiver            | The account number of the receiver is indicated in an invalid way (e.g. IBAN and BBAN number are indicated and don't match or IBAN, BIC combination is wrong).
`COUNTRY_INVALID`                   | receiver            | The IBAN or BIC of the receiver refers to a country where no international transfers are possible at the moment.
`INTERBANK_NOT_FOUND`               | interbank           | Provided interbank BIC could not be found.
`INTERNATIONAL_PAYMENTS_DISABLED`   | channelId           | Currently no international payments are possible at all (server setting).

+ Request

    [PaymentInternationalEntry][]

+ Response 200 (application/json)

    [PaymentInternational][]


# Group Payment Templates
Payment order Templates related resources of *Banking Services API*.

## PaymentTemplate [/netbanking/my/templates/{id}]
Payment Template resource represents one single template for payment order with predefined values. Therefore this resource is very similar to Payment resource, additional field *name* is introduced, the Payment fields *referenceId*, *executionDate*, *modificationDate* and *state* are missing, because have no sense for template.

Description of all possible **PaymentTemplate** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                              | Expected values/format                                                                                                                                                             |
|-------|-------------------|-----------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | id                | TEXT            | M        | Internal identifier of payment template in BE system.                                                                                                                                              |                                                                                                                                                                                    |
| 1     | name              | TEXT            | O        | User defined name of template.                                                                                                                                                                     |                                                                                                                                                                                    |
| 1     | orderCategory     | ENUM            | M        | Payment order category determines whether payment template is domestic, SEPA, international or inside the bank (domestic, but could be different processing) or between accounts of the same user. | ENUM values: [DOMESTIC, SEPA, INTERNATIONAL, OWN_TRANSFER, INTRA_BANK] (should be used instead of flags in current George AT), plus internal `INTRA_BANK`                          |
| 1     | sender            | ACCOUNTNO       | O        | Account number of the sender (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                              | IBAN format for AT, SK, local bank number for CZ                                                                                                                                   |
| 1     | senderName        | TEXT            | O        | Name of sender in payment template                                                                                                                                                                 |                                                                                                                                                                                    |
| 1     | senderAddress     | ARRAY of TEXT   | O        | Sender's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International)        |                                                                                                                                                                                    |
| 1     | senderReference   | TEXT            | C        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                   |                                                                                                                                                                                    |
| 1     | symbols           | structure       | C        | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####).                             |                                                                                                                                                                                    |
| 2     | variableSymbol    | TEXT            | O        | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                              |                                                                                                                                                                                    |
| 2     | specificSymbol    | TEXT            | O        | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                              |                                                                                                                                                                                    |
| 2     | constantSymbol    | TEXT            | O        | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                |                                                                                                                                                                                    |
| 1     | receiver          | ACCOUNTNO       | M        | Account number of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code). |                                                                                                                                                                                    |
| 1     | receiverName      | TEXT            | O        | Name of receiver in payment template                                                                                                                                                               |                                                                                                                                                                                    |
| 1     | receiverAddress   | ARRAY of TEXT   | O        | Receiver's address. Array of 3x text fields with max 35 characters each. First text - Street, Second text - ZIP code and City, Third text - Country code, name. (Used only for International)      |                                                                                                                                                                                    |
| 1     | amount            | AMOUNT          | O        | Payment template amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                          |                                                                                                                                                                                    |
| 1     | additionalInfo    | One Of          | O        | Payment Additional info structure with exclusive elements - Only One Of them could be used in the payment template. This attribute corresponds to SEPA/SWIFT field remittanceInformation.          |                                                                                                                                                                                    |
| 2     | text4x35          | ARRAY of TEXT   | C        | Array of additional text fields, max 4x 35 characters. Payment description, message for receiver.                                                                                                  |                                                                                                                                                                                    |
| 2     | paymentReference  | TEXT            | C        | Payment reference used to identify payment order on receiver side. (Used only for SEPA payments)                                                                                                   |                                                                                                                                                                                    |
| 1     | phoneNumber       | TEXT            | O        | Phone number in full format: "+"+countryCallingCode+areaCallingCode+phoneNumber                                                                                                                    | Format: "+43 666 818 3434" or "+421 911 335 335"                                                                                                                                   |
| 1     | icon              | TEXT            | O        | Icon image selected by user a linked with template, it will be url to image or ID to image store                                                                                                   |                                                                                                                                                                                    |
| 1     | flags             | FLAGS           | O        | Array of optional Flag values depends on Payment order category, type                                                                                                                              | FLAGS: possible values - see table below                                                                                                                                           |

The following flags can be applied to field *flags* in **PaymentTemplate** resource:

Flag         | Description
-------------|-----------------------------------------------
`urgent`     | Flag indicating urgent payment order (in SEPA, SWIFT and maybe also in local bank clearing systems) requested in template
`mobile`     | This template is for use in mobile apps, like current netbanking app.

+ Parameters
    + id (TEXT) ... ID internal identifier of payment template used as part of URI.

+ Model

    + Body

            {
                "id": "4372177879",
                "name": "my super template for payment to Max",
                "orderCategory": "SEPA",
                "sender": {
                    "iban": "AT482011100000005702",
                    "bic": "GIBAATWWXXX"
                },
                "senderName": "Dkffr. Manfred Dichtl",
                "receiver": {
                    "iban": "AT961100000976007260",
                    "bic": "BKAUATWWXXX"
                },
                "receiverName": "Max Mustermann",
                "amount": {
                    "value": 250,
                    "precision": 2,
                    "currency": "EUR"
                },
                "additionalInfo": {
                    "paymentReference": "PayRef xxx-2014"
                },
                "flags": [ "urgent" ]
            }

### Get a one single payment template [GET]
Returns the information about one specific payment template based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**PaymentTemplate** resource containing details of one user payment template identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [PaymentTemplate][]


## PaymentTemplateList [/netbanking/my/templates{?size,page,sort,order}]
Resource Payment Template List represents collection of all payment templates stored for user.
This resource consists of paging attributes and array of *embedded* **PaymentTemplate** resource items.

Description of **PaymentTemplateList** resource attributes: 

| Level | Attribute name | Type/Enum                | Mand/Opt | Attribute description                                              | Expected values/format   |
|-------|----------------|--------------------------|----------|--------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER                  | M        | Page number of returned page, starting from 0 for the first page   |                          |
| 1     | pageCount      | INTEGER                  | M        | Total number of pages of defined size                              |                          |
| 1     | nextPage       | INTEGER                  | O        | Page number of following page (provided only when exist)           |                          |
| 1     | pageSize       | INTEGER                  | M        | Provided or defaulted page size                                    |                          |
| 1     | templates      | ARRAY of PaymentTemplate | O        | Array of payment templates stored for user (could be empty)        |                          |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are only: `id`, `name`, `receiver`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 2,
                "nextPage": 1,
                "pageSize": 5,
                "templates": [
                    {
                        "id": "4372177879",
                        "name": "my super template for payment to Max",
                        "orderCategory": "SEPA",
                        "sender": {
                            "iban": "AT482011100000005702",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "Dkffr. Manfred Dichtl",
                        "receiver": {
                            "iban": "AT961100000976007260",
                            "bic": "BKAUATWWXXX"
                        },
                        "receiverName": "Max Mustermann",
                        "amount": {
                            "value": 250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "additionalInfo": {
                            "paymentReference": "PayRef xxx-2014"
                        },
                        "flags": [ "urgent" ]
                    },
                    {
                        "id": "4386940983",
                        "name": "template for Dubai"
                        "orderCategory": "INTERNATIONAL",
                        "sender": {
                            "iban": "AT622011100000000018",
                            "bic": "GIBAATWWXXX"
                        },
                        "senderName": "Mag. A. M. Mittermuster oder Felix",
                        "receiver": {
                            "iban": "AE060330000010195511161",
                            "bic": "BOMLAEA0",
                            "countryCode": "AE"
                        },
                        "receiverName": "Dubai Friend",
                        "receiverAddress": {
                            "Sheikh Zayed Rd 666",
                            "Dubai",
                            "United Arab Emirates"
                        },
                        "amount": {
                            "value": 125000,
                            "precision": 2,
                            "currency": "USD"
                        }
                    },
                    {
                        "id": "4386940988",
                        "orderCategory": "SEPA",
                        "receiver": {
                            "iban": "AT961100000976007260",
                            "bic": "BKAUATWWXXX"
                        },
                        "receiverName": "Max Mustermann",
                        "amount": {
                            "value": 250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "additionalInfo": {
                            "paymentReference": "PayRef 754786-2014"
                        }
                    },
                    {
                        "id": "4386940999",
                        "name": "Telecom",
                        "orderCategory": "SEPA",
                        "receiver": {
                            "iban": "AT961100000976007260"
                        },
                        "receiverName": "Austria Telecom",
                        "additionalInfo": {
                            "paymentReference": "Invoice 2014/7463698"
                        }
                    },
                    {
                        "id": "CSAS-11024045",
                        "name": "Nekonecny template",
                        "orderCategory": "DOMESTIC",
                        "senderName": "Pepa Travnicek",
                        "symbols": {
                            "variableSymbol": "0123456789",
                            "specificSymbol": "999999"
                        },
                        "receiver": {
                            "number": "123-123",
                            "bankCode": "0100",
                            "countryCode": "CZ"
                        },
                        "receiverName": "Dan Nekonecny",
                        "amount": {
                            "value": 125000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    }
                ]
            }

### Get a list of payment templates for current user [GET]
Get possibly empty list of all payment templates, that this user has defined. This call is paginated and can be sorted.
This call will be used for initial upload of user payment templates from legacy BE system and transfer data to personal Address Book stored in George BE.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **PaymentTemplateList** with possibly empty (omitted) array of *embedded* **PaymentTemplate** items. All templates (domestic+international) are delivered in one list and can be distinguished by *orderCategory* attribute.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [PaymentTemplateList][]


# Group Direct Debits
Direct Debit and Direct Debit Approval related resources of *Banking Services API*.

## SepaDirectDebitApproval [/netbanking/my/accounts/{id}/sepadirectdebits/{ddId}]
SEPA Direct Debit Approval resource represents one user's single approval for incoming Direct Debit payment orders. This approval could be created based on SEPA DD mandate signed between Creditor and user.

Description of all possible **SepaDirectDebitApproval** resource attributes: 

| Level | Attribute name        | Type/Enum   | Mand/Opt | Attribute description                                                                                                                                                         | Expected values/format                                                            |
|-------|-----------------------|-------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| 1     | id                    | TEXT        | M        | Internal identifier of SEPA Direct Debit Approval in BE system.                                                                                                               |                                                                                   |
| 1     | creditorId            | TEXT        | M        | Direct Debit Creditor Identification (SEPA CID).                                                                                                                              | Predefined SEPA CID format agreed for each SEPA country, e.g. SKccZZZ7nnnnnnnnnn  |
| 1     | creditorName          | TEXT        | M        | Direct Debit Creditor Name, which is linked to CID.                                                                                                                           |                                                                                   |
| 1     | name                  | TEXT        | O        | Direct Debit Approval name, description entered by user.                                                                                                                      |                                                                                   |
| 1     | mandateId             | TEXT        | M        | Direct Debit Mandate Identification, ID of contract/mandate between CID and particular debtor.                                                                                |                                                                                   |
| 1     | mandateDate           | DATE        | O        | Direct Debit Mandate Signature date of contract/mandate between CID and particular debtor.                                                                                    | ISO date format: YYYY-MM-DD                                                       |
| 1     | scheme                | ENUM        | M        | Direct Debit Scheme, value CORE should be used for Retail, B2B (Business-To-Business) is only available to businesses, the payer must not be a private individual (consumer). | ENUM values: [CORE, B2B], default value is CORE                                   |
| 1     | status                | ENUM        | M        | Direct Debit Approval status.                                                                                                                                                 | ENUM values: [ACTIVE, INACTIVE, CANCELLED], default value is ACTIVE               |
| 1     | limit                 | AMOUNT      | O        | Direct Debit limit amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                   |                                                                                   |
| 1     | limitInterval         | ENUM        | O        | Direct Debit limit interval period, in which DD orders amounts are summed and validated against defined limit                                                                 | ENUM values: [ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY]        |
| 1     | validFromDate         | DATE        | O        | Date from when Direct Debit Approval will be valid (could be mandatory in local WebAPI documentation)                                                                         | ISO date format:  YYYY-MM-DD                                                      |
| 1     | validToDate           | DATE        | O        | Date till when Direct Debit Approval will be valid                                                                                                                            | ISO date format:  YYYY-MM-DD                                                      |
| 1     | lastTransactionAmount | AMOUNT      | O        | Direct Debit limit amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                   |                                                                                   |

+ Parameters
    + id (TEXT, required) ... ID internal identifier of user account used as part of URI.
    + ddId (TEXT, required) ... ID internal identifier of direct debit approval linked to provided account used as part of URI.

+ Model

    + Body

            {
                "id": "437217fsddevsd",
                "creditorId": "SK47ZZZ70000000100",
                "creditorName": "ZSE, a.s.",
                "name": "my approval for electricity",
                "mandateId": "9439830",
                "scheme": "CORE",
                "status": "ACTIVE",
                "limit": {
                    "value": 9900,
                    "precision": 2,
                    "currency": "EUR"
                },
                "limitInterval": "MONTHLY",
                "validFromDate": "2015-10-29"
            }

### Get a one single Direct Debit Approval [GET]
Returns the information about one user specific SEPA Direct Debit Approval based on given ID and account ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**SepaDirectDebitApproval** resource containing details of one user Direct Debit Approval identified by parameter account ID and DD approval ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided account ID (in URL) does not exist.
`ID_NOT_FOUND` | ddId     | The provided DD approval ID (in URL) does not exist.
`ID_MISMATCH`  | id, ddId | The provided DD approval ID does not match the account ID (DD approval not for this account).

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [SepaDirectDebitApproval][]


## SepaDirectDebitApprovalList [/netbanking/my/accounts/{id}/sepadirectdebits{?size,page,sort,order}]
Resource SEPA Direct Debit Approval List represents collection of all Sepa Direct Debit Approvals for particular account stored for user.
This resource consists of paging attributes and array of *embedded* **SepaDirectDebitApproval** resource items.

Description of **SepaDirectDebitApprovalList** resource attributes: 

| Level | Attribute name        | Type/Enum                        | Mand/Opt | Attribute description                                              | Expected values/format   |
|-------|-----------------------|----------------------------------|----------|--------------------------------------------------------------------|--------------------------|
| 1     | pageNumber            | INTEGER                          | M        | Page number of returned page, starting from 0 for the first page   |                          |
| 1     | pageCount             | INTEGER                          | M        | Total number of pages of defined size                              |                          |
| 1     | nextPage              | INTEGER                          | O        | Page number of following page (provided only when exist)           |                          |
| 1     | pageSize              | INTEGER                          | M        | Provided or defaulted page size                                    |                          |
| 1     | directDebitApprovals  | ARRAY of SepaDirectDebitApproval | O        | Array of Direct Debit Approvals stored for user (could be empty)   |                          |

+ Parameters
    + id (TEXT, required) ... ID internal identifier of user account used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are only: `id`, `creditorId`, `mandateId`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 5,
                "directDebitApprovals": [
                    {
                        "id": "437217fsddevsd",
                        "creditorId": "SK47ZZZ70000000100",
                        "creditorName": "ZSE, a.s.",
                        "name": "my approval for electricity",
                        "mandateId": "9439830",
                        "scheme": "CORE",
                        "status": "ACTIVE",
                        "limit": {
                            "value": 9900,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "limitInterval": "MONTHLY",
                        "validFromDate": "2015-10-29"
                    },
                    {
                        "id": "437217fsddevddd",
                        "creditorId": "SK58ZZZ70043843834",
                        "creditorName": "Allianz SP, a.s.",
                        "name": "my approval for car insurance",
                        "mandateId": "8748379498429",
                        "scheme": "CORE",
                        "status": "ACTIVE",
                        "limit": {
                            "value": 11000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "limitInterval": "HALFYEARLY",
                        "validFromDate": "2015-01-29"
                    },
                    {
                        "id": "437217fsddevsrerd",
                        "creditorId": "SK58ZZZ70043843834",
                        "creditorName": "Allianz SP, a.s.",
                        "name": "my approval for house insurance",
                        "mandateId": "8748379498433",
                        "scheme": "CORE",
                        "status": "ACTIVE",
                        "limit": {
                            "value": 5000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "limitInterval": "HALFYEARLY",
                        "validFromDate": "2015-01-29"
                    }
                ]
            }

### Get a list of Direct Debit Approvals for current user [GET]
Get possibly empty list of all SEPA Direct Debit Approvals, that this user has defined for provided account ID. This call is paginated and can be sorted.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **SepaDirectDebitApprovalList** with possibly empty (omitted) array of *embedded* **SepaDirectDebitApproval** items.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [SepaDirectDebitApprovalList][]


# Group Standing orders
Standing order related resources of *Banking Services API*.

## StandingOrder [/netbanking/my/accounts/{id}/standingorders/{number}]
Standing Order resource represents one single standing order entered by the user. This order could be one of the types: *Standing order* or *Sweep order*.

Description of **StandingOrder** resource attributes: 

| Level | Attribute name     | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                                | Expected values/format                                                                          |
|-------|--------------------|-----------------|----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| 1     | number             | TEXT            | M        | No       | Internal number of standing/sweep order (provided as response after order creation from BE)                                                                                                          |                                                                                                 |
| 1     | type               | ENUM            | M        | No       | Standing or sweep order type                                                                                                                                                                         | ENUM values: [STANDING_ORDER, SWEEP_ORDER]                                                      |
| 1     | status             | ENUM            | M        | No       | State of standing order presented to user on FE, value is mapped based on provided BE technical states.                                                                                              | ENUM values: [OK, NEXT_EXECUTION_DATE_AUTOMATICALLY_SET, NOT_CHANGEABLE, ADVISOR_CHANGEABLE, IN_EXECUTION, DELETED] DELETED - should be used instead of flag `deleted` in current George AT  |
| 1     | alias              | TEXT60          | O        | Yes      | Alias name of standing order entered by user for his better orientation in standing order list                                                                                                       |                                                                                                 |
| 1     | senderReference    | TEXT140         | O        | Yes      | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                     |                                                                                                 |
| 1     | symbols            | structure       | O        | Yes      | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####).                               |                                                                                                 |
| 2     | variableSymbol     | TEXT10          | O        | Yes      | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                                |                                                                                                 |
| 2     | specificSymbol     | TEXT10          | O        | Yes      | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                                |                                                                                                 |
| 2     | constantSymbol     | TEXT4           | O        | Yes      | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                  |                                                                                                 |
| 1     | receiver           | ACCOUNTNO       | M        | No       | Account number of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)    |                                                                                                 |
| 1     | receiverName       | TEXT35          | O        | No       | Name of receiver of standing payment order                                                                                                                                                           |                                                                                                 |
| 1     | paymentReference   | TEXT140         | O        | Yes      | Payment reference used to identify payment order on receiver side. Payment description.                                                                                                              |                                                                                                 |
| 1     | amount             | AMOUNT          | M        | Yes      | Standing order amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                              |                                                                                                 |
| 1     | startDate          | DATETIME        | M        | No       | Datetime when standing payment order was created, valid from this date (provided by local BE system).                                                                                                | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                                      |
| 1     | nextExecutionDate  | DATE            | M        | Yes      | Date when the standing order will be processed the next time.                                                                                                                                        | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | realExecutionDate  | DATE            | O        | No       | Real date when the standing order will be processed the next time, taking into account holidays and weekends and local BE processing for SO.                                                         | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | executionMode      | ENUM            | M        | Yes      | The execution mode defines when or how standing order will be cancelled, processed the last time.                                                                                                    | ENUM values: [UNTIL_CANCELLATION, UNTIL_DATE, AFTER_MAX_ITERATION_EXCEEDED, AFTER_MAX_AMOUNT_EXCEEDED] - `UNTIL_CANCELLATION` (on user request), `UNTIL_DATE` (lastExecutionDate), `AFTER_MAX_ITERATION_EXCEEDED` (maxIterations), `AFTER_MAX_AMOUNT_EXCEEDED` (maxAmount) |
| 1     | lastExecutionDate  | DATE            | C        | Yes      | Date when the standing order will be processed the last time. Only applicable for executionMode `UNTIL_DATE`, then mandatory.                                                                        | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | maxIterations      | INTEGER         | C        | Yes      | Maximum number of iterations - processing of the standing order. Only applicable for executionMode `AFTER_MAX_ITERATION_EXCEEDED`, then mandatory.                                                   |                                                                                                 |
| 1     | maxAmount          | AMOUNT          | C        | Yes      | Maximum amount to be transferred using the standing order. Only applicable for executionMode `AFTER_MAX_AMOUNT_EXCEEDED`, then mandatory.                                                            |                                                                                                 |
| 1     | executionDueMode   | ENUM            | M        | Yes      | Execution due date of the standing order at the end of the month.                                                                                                                                    | ENUM values: [DUE_DAY_OF_MONTH, DUE_LAST_DAY_OF_MONTH]                                          |
| 1     | executionInterval  | ENUM            | M        | No       | The interval period of standing order execution.                                                                                                                                                     | ENUM values: [DAILY, WEEKLY, MONTHLY, BI_MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, IRREGULAR]     |
| 1     | executedIterations | INTEGER         | O        | No       | Executed number of iterations - processing of the standing order from beginning till current date (Read-only, provided by BE system).                                                                |                                                                                                 |
| 1     | executedAmount     | AMOUNT          | O        | No       | Executed amount - sum of all transferred amounts using this standing order so far (Read-only, provided by BE system).                                                                                |                                                                                                 |
| 1     | stoppages          | ARRAY of ENUM   | O        | Yes      | List of months when no payment order is generated from standing order (Only applicable with interval period IRREGULAR, when stoppages is used, then no break should be provided).                    | ENUM values: [JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER] |
| 1     | break              | structure       | O        | Yes      | Structure for break period in generation payment order from this standing order. (When break is used, then no stoppages should be provided).                                                         |                                                                                                 |
| 2     | validFromDate      | DATE            | M        | Yes      | Date from when break period will be valid, the standing order will not be processed from this date.                                                                                                  | ISO date format:  YYYY-MM-DD                                                                    |
| 2     | validToDate        | DATE            | M        | Yes      | Date till when break period will be valid, the standing order will not be processed till this date.                                                                                                  | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | intervalDueDay     | INTEGER         | O        | Yes      | Due date day in execution interval of standing order processing.                                                                                                                                     | Possible values: 1 for DAILY, 1-7 for WEEKLY, 1-31 for MONTHLY, QUARTERLY, HALFYEARLY, YEARLY   |
| 1     | intervalDueMonth   | INTEGER         | O        | Yes      | Due date month in execution interval of standing order processing.                                                                                                                                   | Possible values: N/A for DAILY, WEEKLY, MONTHLY, 1-3 for QUARTERLY, 1-6 for HALFYEARLY, 1-12 for YEARLY |
| 1     | daysOfRepetition   | INTEGER         | O        | Yes      | Maximum number of days of repetition of standing order processing on BE in case of insufficient founds on account.                                                                                   | Possible value: 1-14 (local specific)                                                           |
| 1     | channelId          | ENUM            | O        | Yes      | ID of the channel via which this standing order was entered/modified the last time.                                                                                                                  | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, BACKEND, UNKNOWN] |
| 1     | applicationId      | ENUM            | O        | Yes      | ID of the application via which this standing order was entered/modified the last time.                                                                                                              | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, BUSINESS24, TELEPHONE_BANKER, VIDEO_BANKER, BRANCH_FE, UNKNOWN] |
| 1     | flags              | FLAGS           | O        | No       | Array of optional Flag values depends on Payment order category, type                                                                                                                                | FLAGS possible values: `editable`, `deletable`                                                  |

The following flags can be applied to field *flags* in **StandingOrder** resource:

Flag         | Description
-------------|-----------------------------------------------
`editable`   | Flag indicating if standing order can be edited by client
`deletable`  | Flag indicating if standing order can be deleted by client

+ Parameters
    + id (TEXT) ... ID internal identifier of account used as part of URI.
    + number (TEXT) ... Standing order number identifier used as part of URI.

+ Model

    + Body

            {
                "number": "1007101",
                "type": "STANDING_ORDER",
                "status": "OK",
                "alias": "Appartment rental payment",
                "senderReference": "Appartment 234",
                "receiver": {
                    "iban": "AT961100000976007260",
                    "bic": "BKAUATWWXXX"
                },
                "receiverName": "Max Mustermann",
                "paymentReference": "PayRef 754786-2014"
                "amount": {
                    "value": 102250,
                    "precision": 2,
                    "currency": "EUR"
                },
                "startDate": "2015-03-09T05:21:00+02:00",
                "nextExecutionDate": "2015-04-09",
                "executionMode": "UNTIL_DATE",
                "lastExecutionDate": "2016-04-09",
                "executionDueMode": "DUE_DAY_OF_MONTH",
                "executionInterval": "MONTHLY",
                "intervalDueDay": 9,
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE",
                "flags": [
                    "editable",
                    "deletable"
                ]
            }

### Get a one single standing order [GET]
Returns the information about one specific standing/sweep payment order entered by user based on given account ID and standing order number.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**StandingOrder** resource containing details of one user standing order identified by account ID and standing order number.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided account ID does not exist.
`ID_NOT_FOUND` | number   | The provided standing number does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [StandingOrder][]

### Update one specific standing order [PUT]
Allows to change one specific standing order or sweep order identified by account ID and standing order number. Receiver account and interval period couldn't be changed, only editable attributes can be modified by user. Change of standing order must be signed.
Even though other (not editable) fields are not stored they must fulfill the validation-criteria of StandingOrder-Object. *number* in URL and *number* field in payload: These fields must refer to the same standing order, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**StandingOrder** resource with updated attributes.

#### Reply
A **StandingOrder** resource with updated details of one standing order identified by account ID and standing order number.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum     | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|---------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | standingOrder           | StandingOrder | M        | Standing order object                                  |                                                                |
| 1     | signInfo                | SIGNINFO      | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope             | Purpose
-----------------|-------------------|------------------------------------
`ID_NOT_FOUND`   | id                | The provided account ID does not exist.
`ID_NOT_FOUND`   | number            | The provided standing number does not exist.
`ID_MISMATCH`    | number            | The given number in payload doesn’t match to the number in URI.
`FIELD_TOO_LONG` | alias             | Length of the provided alias is greater than 60.
`FIELD_EMPTY`    | mandatory items   | Particular mandatory field (provided in error.scope) is empty
`INTEGRITY`      | lastExecutionDate | When executionMode is `UNTIL_DATE`, lastExecutionDate must be provided
`INTEGRITY`      | maxIterations     | When executionMode is `AFTER_MAX_ITERATION_EXCEEDED`, maxIterations must be provided
`INTEGRITY`      | maxAmount         | When executionMode is `AFTER_MAX_AMOUNT_EXCEEDED`, maxAmount must be provided
`INTEGRITY`      | id                | sender Account has insufficient permissions for domestic/international standing order operation
`FIELD_INVALID`  | amount.currency   | Invalid currency
`VALUE_INVALID`  | amount.value      | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`FIELD_INVALID`  | nextExecutionDate | Invalid the next execution date
`FIELD_INVALID`  | lastExecutionDate | Invalid the last execution date

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "number": "1007101",
                "type": "STANDING_ORDER",
                "status": "OK",
                "alias": "NEW NAME - Appartment rental",
                "senderReference": "NEW Appartment 666",
                "receiver": {
                    "iban": "AT961100000976007260",
                    "bic": "BKAUATWWXXX"
                },
                "receiverName": "Max Mustermann",
                "paymentReference": "PayRef 754786-2014"
                "amount": {
                    "value": 109990,
                    "precision": 2,
                    "currency": "EUR"
                },
                "startDate": "2015-03-09T05:21:00+02:00",
                "nextExecutionDate": "2015-04-15",
                "executionMode": "UNTIL_DATE",
                "lastExecutionDate": "2016-04-15",
                "executionDueMode": "DUE_DAY_OF_MONTH",
                "executionInterval": "MONTHLY",
                "break": {
                    "validFromDate": "2015-07-01",
                    "validToDate": "2015-09-05"
                },
                "intervalDueDay": 15,
                "daysOfRepetition": 5,
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE"
            }

+ Response 200 (application/json)

    + Body

            {
                "standingOrder": {
                    "number": "1007101",
                    "type": "STANDING_ORDER",
                    "status": "OK",
                    "alias": "NEW NAME - Appartment rental",
                    "senderReference": "NEW Appartment 666",
                    "receiver": {
                        "iban": "AT961100000976007260",
                        "bic": "BKAUATWWXXX"
                    },
                    "receiverName": "Max Mustermann",
                    "paymentReference": "PayRef 754786-2014"
                    "amount": {
                        "value": 109990,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "startDate": "2015-03-09T05:21:00+02:00",
                    "nextExecutionDate": "2015-04-15",
                    "executionMode": "UNTIL_DATE",
                    "lastExecutionDate": "2016-04-15",
                    "executionDueMode": "DUE_DAY_OF_MONTH",
                    "executionInterval": "MONTHLY",
                    "break": {
                        "validFromDate": "2015-07-01",
                        "validToDate": "2015-09-05"
                    },
                    "intervalDueDay": 15,
                    "daysOfRepetition": 5,
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "flags": [
                        "editable",
                        "deletable"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "044268356190000004006827"
                }
            }

### Remove one specific standing order [DELETE]
Delete one specific standing or sweep order identified by account ID and standing order number.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of DELETE resource attributes:

| Level | Attribute name          | Type/Enum         | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | signInfo                | SIGNINFO          | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided account ID does not exist.
`ID_NOT_FOUND` | number   | The provided standing number does not exist.
`INTEGRITY`    | number   | The standing order is not deletable.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    + Body

            {
                "signInfo": {
                    "state": "OPEN",
                    "signId": "044268356190000004006828"
                }
            }


## StandingOrderEntry [/netbanking/my/accounts/{id}/standingorders]
Standing Order Entry resource represents creation of new single Standing or Sweep payment order entered by the user. Resource uses subset of attributes of embedded **StandingOrder** resource.

Description of **StandingOrderEntry** resource attributes: 

| Level | Attribute name    | Type/Enum       | Mand/Opt | Attribute description                                                                                                                                                                                | Expected values/format                                                                          |
|-------|-------------------|-----------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| 1     | type              | ENUM            | M        | Standing or sweep order type                                                                                                                                                                         | ENUM values: [STANDING_ORDER, SWEEP_ORDER]                                                      |
| 1     | alias             | TEXT60          | O        | Alias name of standing order entered by user for his better orientation in standing order list                                                                                                       |                                                                                                 |
| 1     | senderReference   | TEXT140         | O        | Optional sender's (payer/collector) reference number, which is transferred to receiver to reconcile payment. This corresponds to SEPA field endToEndInformation.                                     |                                                                                                 |
| 1     | symbols           | structure       | O        | Symbols structure for VS, SS, KS used in CZ and SK (Symbols in SK will be provided only when Sender Reference was filled in format /VS##########/SS##########/KS####).                               |                                                                                                 |
| 2     | variableSymbol    | TEXT10          | O        | Variable symbol (VS) used as payer's reference/invoice ID/customer ID (VS could be masked with * for Card number/PAN)                                                                                |                                                                                                 |
| 2     | specificSymbol    | TEXT10          | O        | Specific symbol (SS) used as payer's reference/customer ID/time period identification                                                                                                                |                                                                                                 |
| 2     | constantSymbol    | TEXT4           | O        | Constant symbol (KS) code used for payment categorization (for local national bank)                                                                                                                  |                                                                                                 |
| 1     | receiver          | ACCOUNTNO       | M        | Account number of the receiver (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code or free text account with bank code/BIC with country code)    |                                                                                                 |
| 1     | receiverName      | TEXT35          | O        | Name of receiver of standing payment order                                                                                                                                                           |                                                                                                 |
| 1     | paymentReference  | TEXT140         | O        | Payment reference used to identify payment order on receiver side. Payment description.                                                                                                              |                                                                                                 |
| 1     | amount            | AMOUNT          | M        | Standing order amount in defined currency (only EUR for SEPA) and with precision (embedded AMOUNT type)                                                                                              |                                                                                                 |
| 1     | nextExecutionDate | DATE            | M        | Date when the standing order will be processed the first time for created standing order                                                                                                             | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | executionMode     | ENUM            | M        | The execution mode defines when or how standing order will be cancelled, processed the last time.                                                                                                    | ENUM values: [UNTIL_CANCELLATION, UNTIL_DATE, AFTER_MAX_ITERATION_EXCEEDED, AFTER_MAX_AMOUNT_EXCEEDED] - `UNTIL_CANCELLATION` (default value), `UNTIL_DATE` (lastExecutionDate), `AFTER_MAX_ITERATION_EXCEEDED` (maxIterations), `AFTER_MAX_AMOUNT_EXCEEDED` (maxAmount)|
| 1     | lastExecutionDate | DATE            | C        | Date when the standing order will be processed the last time. Only applicable for executionMode `UNTIL_DATE`, then mandatory.                                                                        | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | maxIterations     | INTEGER         | C        | Maximum number of iterations - processing of the standing order. Only applicable for executionMode `AFTER_MAX_ITERATION_EXCEEDED`, then mandatory.                                                   |                                                                                                 |
| 1     | maxAmount         | AMOUNT          | C        | Maximum amount to be transferred using the standing order. Only applicable for executionMode `AFTER_MAX_AMOUNT_EXCEEDED`, then mandatory.                                                            |                                                                                                 |
| 1     | executionDueMode  | ENUM            | M        | Execution due date of the standing order at the end of the month.                                                                                                                                    | ENUM values: [DUE_DAY_OF_MONTH, DUE_LAST_DAY_OF_MONTH]                                          |
| 1     | executionInterval | ENUM            | M        | The interval period of standing order execution.                                                                                                                                                     | ENUM values: [DAILY, WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, IRREGULAR]                 |
| 1     | stoppages         | ARRAY of ENUM   | O        | List of months when no payment order is generated from standing order (Only applicable with interval period IRREGULAR, when stoppages is used, then no break should be provided).                    | ENUM values: [JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER] |
| 1     | break             | structure       | O        | Structure for break period in generation payment order from this standing order. (When break is used, then no stoppages should be provided).                                                         |                                                                                                 |
| 2     | validFromDate     | DATE            | M        | Date from when break period will be valid, the standing order will not be processed from this date.                                                                                                  | ISO date format:  YYYY-MM-DD                                                                    |
| 2     | validToDate       | DATE            | M        | Date till when break period will be valid, the standing order will not be processed till this date.                                                                                                  | ISO date format:  YYYY-MM-DD                                                                    |
| 1     | intervalDueDay    | INTEGER         | O        | Due date day in execution interval of standing order processing.                                                                                                                                     | Possible values: 1 for DAILY, 1-7 for WEEKLY, 1-31 for MONTHLY, QUARTERLY, HALFYEARLY, YEARLY   |
| 1     | intervalDueMonth  | INTEGER         | O        | Due date month in execution interval of standing order processing.                                                                                                                                   | Possible values: N/A for DAILY, WEEKLY, MONTHLY, 1-3 for QUARTERLY, 1-6 for HALFYEARLY, 1-12 for YEARLY |
| 1     | daysOfRepetition  | INTEGER         | O        | Maximum number of days of repetition of standing order processing on BE in case of insufficient founds on account.                                                                                   | Possible value: 1-14 (local specific)                                                           |
| 1     | channelId         | ENUM            | O        | ID of the channel via which this standing order was entered/modified the last time.                                                                                                                  | ENUM values: [NET_BANKING, MOBILE_BANKING, HOME_BANKING, CALL_CENTRE, VIDEO_BANKING, BRANCH, BACKEND, UNKNOWN] |
| 1     | applicationId     | ENUM            | O        | ID of the application via which this standing order was entered/modified the last time.                                                                                                              | ENUM values: [GEORGE, INTERNET_BANKING, GEORGE_GO, BUSINESS24, TELEPHONE_BANKER, VIDEO_BANKER, BRANCH_FE, UNKNOWN] |

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "type": "STANDING_ORDER",
                "alias": "Kindergarden - Jacob",
                "senderReference": "Jacob - kindergarden monthly fee",
                "receiver": {
                    "iban": "AT352011100000003608"
                },
                "receiverName": "Happy Kids - Wien",
                "paymentReference": "PayRef 20140708"
                "amount": {
                    "value": 40900,
                    "precision": 2,
                    "currency": "EUR"
                },
                "nextExecutionDate": "2015-04-20",
                "executionMode": "UNTIL_DATE",
                "lastExecutionDate": "2017-06-30",
                "executionDueMode": "DUE_DAY_OF_MONTH",
                "executionInterval": "MONTHLY",
                "intervalDueDay": 20,
                "channelId": "NET_BANKING",
                "applicationId": "GEORGE",
            }

### Create a one single Standing order [POST]
Create one new Standing or Sweep payment order entered by user. In case the call fails with a serverside field check, the check should continue as far as possible and as many errors as possible should be detected at one POST. However, there is no guarantee that the API delivers all wrong fields at once for one POST call.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**StandingOrderEntry** resource with all required data entered by user in FE application.

#### Reply
**StandingOrder** resource stored locally (in BE system or Payment Store&Forward local solution) containing details of one user standing payment order.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of POST resource attributes:

| Level | Attribute name          | Type/Enum         | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | standingOrder           | StandingOrder     | M        | StandingOrder object                                   |                                                                |
| 1     | signInfo                | SIGNINFO          | M        | SignInfo Details                                       |                                                                |

#### Error codes
If user is not allowed to create this standing order, HTTP 403 is returned, and the error code explains why:

Error code                          | Scope             | Purpose
------------------------------------|-------------------|------------------------------------
`ID_NOT_FOUND`                      | id                | There is no account with the given ID
`FIELD_EMPTY`                       | mandatory items   | Particular mandatory field (provided in error.scope) is empty
`INTEGRITY`                         | lastExecutionDate | When executionMode is `UNTIL_DATE`, lastExecutionDate must be provided
`INTEGRITY`                         | maxIterations     | When executionMode is `AFTER_MAX_ITERATION_EXCEEDED`, maxIterations must be provided
`INTEGRITY`                         | maxAmount         | When executionMode is `AFTER_MAX_AMOUNT_EXCEEDED`, maxAmount must be provided
`INTEGRITY`                         | id                | sender Account has insufficient permissions for domestic/international standing order operation
`FIELD_INVALID`                     | amount.currency   | Invalid currency
`VALUE_INVALID`                     | amount.value      | Amount is less or equal 0 or larger than 999999999.99 (should be local parameter)
`FIELD_INVALID`                     | type              | Invalid type provided
`FIELD_INVALID`                     | nextExecutionDate | Invalid the next execution date
`FIELD_INVALID`                     | lastExecutionDate | Invalid the last execution date
`FIELD_INVALID`                     | receiver.iban/bic | Invalid receiver account data - IBAN, BIC or number, bankCode

+ Request

    [StandingOrderEntry][]

+ Response 200 (application/json)

    + Body

            {
                "standingOrder": {
                    "number": "1033333",
                    "type": "STANDING_ORDER",
                    "status": "OK",
                    "alias": "Kindergarden - Jacob",
                    "senderReference": "Jacob - kindergarden monthly fee",
                    "receiver": {
                        "iban": "AT352011100000003608"
                    },
                    "receiverName": "Happy Kids - Wien",
                    "paymentReference": "PayRef 20140708"
                    "amount": {
                        "value": 40900,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "startDate": "2015-04-17T05:21:00+02:00",
                    "nextExecutionDate": "2015-04-20",
                    "executionMode": "UNTIL_DATE",
                    "lastExecutionDate": "2017-06-30",
                    "executionDueMode": "DUE_DAY_OF_MONTH",
                    "executionInterval": "MONTHLY",
                    "intervalDueDay": 20,
                    "channelId": "NET_BANKING",
                    "applicationId": "GEORGE",
                    "flags": [
                        "editable",
                        "deletable"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "044268356190000004446828"
                }
            }


## StandingOrderList [/netbanking/my/accounts/{id}/standingorders{?size,page,sort,order}]
Resource Standing Order List represents collection of all standing orders entered by user and not deleted.
This resource consists of paging attributes and array of *embedded* **StandingOrder** resource items.

Description of **StandingOrderList** resource attributes: 

| Level | Attribute name | Type/Enum              | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER                | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER                | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER                | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER                | M        | Provided or defaulted page size                                      |                          |
| 1     | standingOrders | ARRAY of StandingOrder | O        | Array of standing orders entered by the user (could be empty) (embedded StandingOrder resource) |  |

+ Parameters
    + id (TEXT) ... ID internal identifier of account used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort field is only: `nextExecutionDate`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.
    
+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 3,
                "standingOrders": [
                    {
                        "number": "1007101",
                        "type": "STANDING_ORDER",
                        "status": "OK",
                        "alias": "Appartment rental payment",
                        "senderReference": "Appartment 234",
                        "receiver": {
                            "iban": "AT961100000976007260",
                            "bic": "BKAUATWWXXX"
                        },
                        "receiverName": "Max Mustermann",
                        "paymentReference": "PayRef 754786-2014"
                        "amount": {
                            "value": 102250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "startDate": "2015-01-09T05:21:00+02:00",
                        "nextExecutionDate": "2015-04-09",
                        "executionMode": "UNTIL_DATE",
                        "lastExecutionDate": "2016-04-09",
                        "executionDueMode": "DUE_DAY_OF_MONTH",
                        "executionInterval": "MONTHLY",
                        "intervalDueDay": 9,
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "flags": [
                            "editable",
                            "deletable"
                        ]
                    },
                    {
                        "number": "1007979",
                        "type": "STANDING_ORDER",
                        "status": "OK",
                        "alias": "Kindergarden - Olivia",
                        "senderReference": "Olivia - kindergarden monthly fee",
                        "receiver": {
                            "iban": "AT352011100000003608",
                            "bic": "GIBAATWWXXX"
                        },
                        "receiverName": "Happy Kids - Wien",
                        "paymentReference": "PayRef 20141123"
                        "amount": {
                            "value": 44400,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "startDate": "2015-01-15T05:21:00+02:00",
                        "nextExecutionDate": "2015-04-20",
                        "executionMode": "UNTIL_DATE",
                        "lastExecutionDate": "2016-06-30",
                        "executionDueMode": "DUE_DAY_OF_MONTH",
                        "executionInterval": "IRREGULAR",
                        "stoppages": [
                            "JULY",
                            "AUGUST"
                        ],
                        "intervalDueDay": 20,
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "flags": [
                            "deletable"
                        ]
                    },
                    {
                        "number": "1009999",
                        "type": "STANDING_ORDER",
                        "status": "OK",
                        "alias": "My Saving",
                        "senderReference": "Monthly saving",
                        "receiver": {
                            "iban": "AT662011100001237957",
                            "bic": "GIBAATWWXXX"
                        },
                        "amount": {
                            "value": 77700,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "startDate": "2015-04-01T05:21:00+02:00",
                        "nextExecutionDate": "2015-05-01",
                        "executionMode": "AFTER_MAX_AMOUNT_EXCEEDED",
                        "maxAmount": {
                            "value": 999900,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "executionDueMode": "DUE_DAY_OF_MONTH",
                        "executionInterval": "MONTHLY",
                        "intervalDueDay": 1,
                        "daysOfRepetition": 5,
                        "channelId": "NET_BANKING",
                        "applicationId": "GEORGE",
                        "flags": [
                            "deletable"
                        ]
                    }
                ]
            }

### Get a list of standing orders for provided account [GET]
Get possibly empty list of all standing orders for identified account, that have been entered by user through all distribution channels. This call is paginated and can be sorted.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **StandingOrderList** with possibly empty (omitted) array of *embedded* **StandingOrder** items.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided account ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [StandingOrderList][]


# Group Statements
Statement related resources of *Banking Services API*.

## StatementList [/netbanking/my/accounts/{id}/statements{?size,page,sort,order}]
Statement List resource represents collection of statements created for single account identified by ID.
This resource consists of paging attributes and array of *embedded* **Statement** items.

Description of **StatementList** resource attributes: 

| Level | Attribute name    | Type/Enum           | Mand/Opt | Attribute description                                                                | Expected values/format                       |
|-------|-------------------|---------------------|----------|--------------------------------------------------------------------------------------|----------------------------------------------|
| 1     | pageNumber        | INTEGER             | M        | Page number of returned page, starting from 0 for the first page                     |                                              |
| 1     | pageCount         | INTEGER             | M        | Total number of pages of defined size                                                |                                              |
| 1     | nextPage          | INTEGER             | O        | Page number of following page (provided only when exist)                             |                                              |
| 1     | pageSize          | INTEGER             | M        | Provided or defaulted page size                                                      |                                              |
| 1     | statements        | ARRAY of            | O        | Array of account's statements (could be empty) (embedded Statement)                  |                                              |
| 2     | id                | TEXT                | M        | Internal identifier of statement in BE system.                                       |                                              |
| 2     | number            | INTEGER             | M        | Statement sequence number (used for one statement definition)                        |                                              |
| 2     | setupId           | TEXT                | O        | Internal identifier of statement definition setup in BE system.                      |                                              |
| 2     | minBookingDate    | DATE                | O        | Minimal booking date in statement period                                             | ISO date format: YYYY-MM-DD                  |
| 2     | maxBookingDate    | DATE                | O        | Maximal booking date in statement period                                             | ISO date format: YYYY-MM-DD                  |
| 2     | statementDate     | DATETIME            | M        | Timestamp of statement creation                                                      | ISO dateTime format: YYYY-MM-DDThh:mm:ssZ    |
| 2     | previousBalance   | AMOUNT              | O        | Previous account balance at the beginning of this statement period (embedded AMOUNT) | Fields value, precision, currency            |
| 2     | newBalance        | AMOUNT              | O        | New account balance at the end of this statement period (embedded AMOUNT)            | Fields value, precision, currency            |
| 2     | totalCredits      | AMOUNT              | O        | Total sum of credits on account during this statement period (embedded AMOUNT)       | Fields value, precision, currency            |
| 2     | totalDebits       | AMOUNT              | O        | Total sum of debits on account during this statement period (embedded AMOUNT)        | Fields value, precision, currency            |
| 2     | periodicity       | ENUM                | O        | Periodicity of account statement creation                                            | ENUM values: [ONCE, DAILY, WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY] (local specific) |
| 2     | format            | ENUM                | O        | Format of account statement                                                          | ENUM values: [PDF_A4, TXT_A4, XML_SEPA, OFX] - OFX as Open Financial Exchange file (local specific)  |
| 2     | language          | ENUM                | O        | Language version of created statement.                                               | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] (local specific) |

+ Parameters
    + id (TEXT) ... ID internal identifier of account used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort field is only `statementDate`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default.
    
+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 2,
                "statements": [
                    {
                        "id": "201302520130621161819",
                        "number": 25,
                        "minBookingDate": "2013-06-14",
                        "maxBookingDate": "2013-06-21",
                        "statementDate": "2013-06-21T14:18:19Z",
                        "previousBalance": {
                            "value": -10599216226,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "newBalance": {
                            "value": -159977726,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "periodicity": "WEEKLY",
                        "language": "de"
                    },
                    {
                        "id": "201302620130628142140",
                        "number": 26,
                        "minBookingDate": "2013-06-24",
                        "maxBookingDate": "2013-06-27",
                        "statementDate": "2013-06-28T12:21:40Z",
                        "previousBalance": {
                            "value": -159977726,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "newBalance": {
                            "value": -10626016690,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "periodicity": "WEEKLY",
                        "language": "de"
                    }
                ]
            }

### Get a list of statements for account [GET]
Get possibly empty list of statements to identified account. Statement history limitations are local country specific (e.g. only the last 13 months in AT). 
This call is paginated and can be sorted.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **StatementList** with possibly empty array of *embedded* **Statement** items.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [StatementList][]


## StatementFile [/netbanking/my/accounts/{id}/statements/download{?format,statementId*}]
Statement File resource represents one binary account statement file in specific format containing statement(s) identified by ID(s).
This resource has no JSON payload, it is only binary data file.

+ Parameters
    + id (TEXT, required) ... ID internal identifier of account used as part of URI.
    + format (enum[TEXT], optional) 
    
        Statement format used as URI parameter. Format type could be empty, when particular statementId has only one possible format of statement. Possible ENUM values:
        <br>`PDF_A4` - Requested format of the file is PDF, DIN-A4 format - Default value
        <br>`PDF_A4-3` - Requested format of the file is PDF, DIN-A4 format - statement divided into three parts per page
        <br>`PDF_A6` - Requested format of the file is PDF, DIN-A6 format
        <br>`TXT_A4` - Requested format of the file is TXT, DIN-A4 format
        <br>`TXT_A4-3` - Requested format of the file is TXT, DIN-A4 format - statement divided into three parts per page
        <br>`TXT_A6` - Requested format of the file is TXT, DIN-A6 format
        <br>`OFX` - Requested format is Open Financial Exchange file - maximum one statement can be selected to download at once
        <br>`XML_SEPA` - Requested format is SEPA XML file

    + statementId (TEXT, required) ... One or list of composite Statement IDs separated by coma and used as URI parameter. If more IDs are provided then one response file will contain all requested statements. Local specification could limit request to only one ID.

### Download statement file for account [POST]
Get an (aggregated) account statement file in a specific format for download, for given account and statement ID(s). Format type limitations are local country specific (e.g. only `PDF_A4` and `XML_SEPA` in Slovakia).

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
The binary representation of an account statement file, with a “Content-Disposition” header of type attachment (including the filename), in order to instruct the browser to open a save dialog.
The filename is composed of: account IBAN + `_` + statement no. 1 + … `_` + statement no. n (maximal length of filename is 100 incl. extension).

Description of POST resource attributes:
Attachment.

#### Error codes
Error code                      | Scope         | Purpose
--------------------------------|---------------|------------------------------------
`ID_NOT_FOUND`                  | id            | Account does not exist or does not belong to the user. HTTP Error 404
`ACCOUNT_QUERY_NOT_ALLOWED`     | id, flag      | User does not have the permission to view transactions and statements. HTTP Error 403
`STATEMENT_NOT_FOUND`           | statementId   | One of the requested statements cannot be found. HTTP Error 404
`NO_STATEMENT_SELECTED`         | statementId   | No statements has been selected for download. HTTP Error 400
`TOO_MANY_STATEMENTS_SELECTED`  | statementId   | Too many statements have been selected for download (relevant for OFX format). HTTP Error 400
`FIELD_INVALID`                 | statementId   | The requested statement contains too many transactions. HTTP Error 400
`INVALID_FORMAT`                | format        | Value of the format parameter is not valid. HTTP Error 400

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200

    + Headers

            Content-Disposition: attachment; filename=filename.ext
            Content-Type: application/octet-stream


## StatementFileSigned [/netbanking/my/accounts/{id}/statements/signed/download{?format,statementId}]
Statement File Signed resource represents one binary account statement file in specific format containing statement identified by ID, while provided file is signed by Bank (using certificate, electronic signature).
This resource has no JSON payload, it is only binary data file.

+ Parameters
    + id (TEXT, required) ... ID internal identifier of account used as part of URI.
    + format (enum[TEXT], optional) 
    
        Statement format used as URI parameter. Format type could be empty, when particular statementId has only one possible format of statement. Possible ENUM values:
        <br>`PDF_A4` - Requested format of the file is PDF, DIN-A4 format - Default value
        <br>`TXT_A4` - Requested format of the file is TXT, DIN-A4 format
        <br>`OFX` - Requested format is Open Financial Exchange file - maximum one statement can be selected to download at once
        <br>`XML_SEPA` - Requested format is SEPA XML file

    + statementId (TEXT, required) ... One Statement ID used as URI parameter.

### Download signed statement file for account [POST]
Get an account statement file in a specific format for download, for given account and statement ID, which is signed by the Bank. Format type limitations are local country specific (e.g. only `PDF_A4` and `XML_SEPA` in Slovakia).

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
The binary representation of an account statement file, with a “Content-Disposition” header of type attachment (including the filename), in order to instruct the browser to open a save dialog.
The filename is composed of: account IBAN + `_` + statement no.

Description of POST resource attributes:
Attachment.

#### Error codes
Error code                      | Scope         | Purpose
--------------------------------|---------------|------------------------------------
`ID_NOT_FOUND`                  | id            | Account does not exist or does not belong to the user. HTTP Error 404
`ACCOUNT_QUERY_NOT_ALLOWED`     | id, flag      | User does not have the permission to view transactions and statements. HTTP Error 403
`STATEMENT_NOT_FOUND`           | statementId   | Requested statement ID cannot be found. HTTP Error 404
`NO_STATEMENT_SELECTED`         | statementId   | No statements has been selected for download. HTTP Error 400
`FIELD_INVALID`                 | statementId   | The requested statement contains too many transactions. HTTP Error 400
`INVALID_FORMAT`                | format        | Value of the format parameter is not valid. HTTP Error 400

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200

    + Headers

            Content-Disposition: attachment; filename=filename.ext
            Content-Type: application/octet-stream


## CardStatementList [/netbanking/my/cards/{id}/mainaccount/{aId}/statements{?size,page,sort,order}]
Credit card Statement List resource represents collection of statements created for Credit card shadow account identified by card ID and its main account ID.
This resource consists of paging attributes and array of *embedded* **Statement** items.

Description of **CardStatementList** resource attributes: 

| Level | Attribute name    | Type/Enum           | Mand/Opt | Attribute description                                                    | Expected values/format                       |
|-------|-------------------|---------------------|----------|--------------------------------------------------------------------------|----------------------------------------------|
| 1     | pageNumber        | INTEGER             | M        | Page number of returned page, starting from 0 for the first page         |                                              |
| 1     | pageCount         | INTEGER             | M        | Total number of pages of defined size                                    |                                              |
| 1     | nextPage          | INTEGER             | O        | Page number of following page (provided only when exist)                 |                                              |
| 1     | pageSize          | INTEGER             | M        | Provided or defaulted page size                                          |                                              |
| 1     | statements        | ARRAY of            | O        | Array of Credit card shadow account statements (could be empty) (embedded Statement) |                                              |
| 2     | id                | TEXT                | M        | Internal identifier of statement in BE system.                           |                                              |
| 2     | number            | INTEGER             | M        | Statement sequence number (used for one statement definition)            |                                              |
| 2     | setupId           | TEXT                | O        | Internal identifier of statement definition setup in BE system.          |                                              |
| 2     | minBookingDate    | DATE                | O        | Minimal booking date in statement period                                 | ISO date format: YYYY-MM-DD                  |
| 2     | maxBookingDate    | DATE                | O        | Maximal booking date in statement period                                 | ISO date format: YYYY-MM-DD                  |
| 2     | statementDate     | DATETIME            | M        | Timestamp of statement creation                                          | ISO dateTime format: YYYY-MM-DDThh:mm:ssZ    |
| 2     | previousBalance   | AMOUNT              | O        | Previous account balance at the beginning of this statement period (embedded AMOUNT)  | Fields value, precision, currency            |
| 2     | newBalance        | AMOUNT              | O        | New account balance at the end of this statement period (embedded AMOUNT)            | Fields value, precision, currency            |
| 2     | totalCredits      | AMOUNT              | O        | Total sum of credits on account during this statement period (embedded AMOUNT)       | Fields value, precision, currency            |
| 2     | totalDebits       | AMOUNT              | O        | Total sum of debits on account during this statement period (embedded AMOUNT)        | Fields value, precision, currency            |
| 2     | periodicity       | ENUM                | O        | Periodicity of account statement creation                                | ENUM values: [DAILY, WEEKLY, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY] (local specific) |
| 2     | format            | ENUM                | O        | Format of account statement                                              | ENUM values: [PDF_A4, TXT_A4, XML_SEPA, OFX] - OFX as Open Financial Exchange file (local specific) |
| 2     | language          | ENUM                | O        | Language version of created statement.                                   | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] (local specific)    |

+ Parameters
    + id (TEXT) ... ID internal identifier of Credit card used as part of URI.
    + aId (TEXT) ... ID internal identifier of Credit card main shadow account used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort field is only `statementDate`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default.
    
+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 2,
                "statements": [
                    {
                        "id": "201402520130621161559",
                        "number": 25,
                        "minBookingDate": "2014-06-01",
                        "maxBookingDate": "2014-06-30",
                        "statementDate": "2014-06-30T14:18:19Z",
                        "previousBalance": {
                            "value": -216226,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "newBalance": {
                            "value": -777026,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "periodicity": "MONTHLY",
                        "language": "cs"
                    },
                    {
                        "id": "201402620130628142140",
                        "number": 26,
                        "minBookingDate": "2014-07-01",
                        "maxBookingDate": "2014-07-31",
                        "statementDate": "2014-07-31T12:21:40Z",
                        "previousBalance": {
                            "value": -777026,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "newBalance": {
                            "value": -6016690,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "periodicity": "MONTHLY",
                        "language": "cs"
                    }
                ]
            }

### Get a list of statements for Credit card [GET]
Get possibly empty list of statements to identified Credit card shadow account. Statement history limitations are local country specific. 
This call is paginated and can be sorted.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **CardStatementList** with possibly empty (omitted) array of *embedded* **Statement** items created for Credit card shadow account.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CardStatementList][]


## CardStatementFileSigned [/netbanking/my/cards/{id}/mainaccount/{aId}/statements/signed/download{?format,statementId}]
Credit card Statement File Signed resource represents one binary Credit card shadow account statement file in specific format containing CC statement identified by ID. Provided file is signed by Bank (using certificate, electronic signature).
This resource has no JSON payload, it is only binary data file.

+ Parameters
    + id (TEXT, required) ... ID internal identifier of Credit card used as part of URI.
    + aId (TEXT, required) ... ID internal identifier of Credit card shadow account used as part of URI.
    + format (enum[TEXT], optional) 
    
        Statement format used as URI parameter. Format type could be empty, when particular statementId has only one possible format of statement. Possible ENUM values:
        <br>`PDF_A4` - Requested format of the file is PDF, DIN-A4 format - Default value
        <br>`TXT_A4` - Requested format of the file is TXT, DIN-A4 format
        <br>`OFX` - Requested format is Open Financial Exchange file - maximum one statement can be selected to download at once
        <br>`XML_SEPA` - Requested format is SEPA XML file

    + statementId (TEXT, required) ... One Statement ID used as URI parameter.

### Download signed statement file for Credit card [POST]
Get an Credit card shadow account statement file in a specific format for download, for given Credit card ID, shadow account ID and statement ID. Provided statement file is signed by the Bank. Format type limitations are local country specific (e.g. only `PDF_A4` and `XML_SEPA` in Slovakia).

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
The binary representation of Credit card shadow account statement file, with a “Content-Disposition” header of type attachment (including the filename), in order to instruct the browser to open a save dialog.
The filename is composed of: Credit card PAN + `_` + statement no.

Description of POST resource attributes:
Attachment.

#### Error codes
Error code                      | Scope         | Purpose
--------------------------------|---------------|------------------------------------
`ID_NOT_FOUND`                  | id, aId       | Card or shadow Account does not exist or does not belong to the user. HTTP Error 404
`ACCOUNT_QUERY_NOT_ALLOWED`     | aId, flag     | User does not have the permission to view transactions and statements. HTTP Error 403
`STATEMENT_NOT_FOUND`           | statementId   | Requested statement ID cannot be found. HTTP Error 404
`NO_STATEMENT_SELECTED`         | statementId   | No statements has been selected for download. HTTP Error 400
`FIELD_INVALID`                 | statementId   | The requested statement contains too many transactions. HTTP Error 400
`INVALID_FORMAT`                | format        | Value of the format parameter is not valid. HTTP Error 400

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200

    + Headers

            Content-Disposition: attachment; filename=filename.ext
            Content-Type: application/octet-stream


# Group Cards
Card-related resources of *Banking Services API*.

## Card [/netbanking/my/cards/{id}]
Card type represents user card product of different card types (credit card, debit/bank card).

Description of **Card** resource/type attributes: 

| Level | Attribute name           | Type/Enum | Mand/Opt | Editable         | Attribute description                                                                                                                                                                                        | Expected values/format                                                                                                                                               |
|-------|--------------------------|-----------|----------|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1     | id                       | TEXT      | M        | No               | Internal identifier of card (unique system ID from BE)                                                                                                                                                       |                                                                                                                                                                      |
| 1     | alias                    | TEXT60    | O        | Yes              | User defined card product alias                                                                                                                                                                              |                                                                                                                                                                      |
| 1     | owner                    | TEXT      | M        | No               | Full Name of the card owner, holder of card                                                                                                                                                                  | format "FirstName MiddleName LastName"                                                                                                                               |
| 1     | number                   | TEXT      | M        | No               | Masked card number (PAN - primary account number), only the first 6 and the last 4 digits are displayed, asterisk is used for the rest of digits.                                                            | ISO 7812 format: "525405******1234"                                                                                                                                  |
| 1     | productCode              | TEXT      | O        | No               | Product code of the internal card (issued by local Erste Bank)                                                                                                                                               | e.g.  "C-SKVIFIRS" in AT                                                                                                                                             |
| 1     | productI18N              | TEXT      | M        | No               | Localized product name of card depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language).                                                 | e.g. "Visa Gold card", "Erste Visa Standard card"                                                                                                                    |
| 1     | expiryDate               | DATE      | O        | No               | Expiration date of particular plastic card (not available for all cards)                                                                                                                                     | ISO date format:  YYYY-MM-DD                                                                                                                                         |
| 1     | validFromDate            | DATE      | O        | No               | Date from which this particular plastic card is valid (not available for all cards)                                                                                                                          | ISO date format:  YYYY-MM-DD                                                                                                                                         |
| 1     | balance                  | AMOUNT    | O        | No               | Disposable balance of current account linked to debit/bank card or Available balance of credit card (disposable balance of shadow account). Not available for all cards or states (locked, closed, unknown). | Fields value, precision, currency                                                                                                                                    |
| 1     | limit                    | AMOUNT    | O        | No               | For credit card: Loan limit for card (shadow) account; For bank/debit card:  maximum limit for bank/debit card (e.g. for electronic wallet transfer/ cashless payment of bank card in AT)                    | Fields value, precision, currency                                                                                                                                    |
| 1     | outstandingAmount        | AMOUNT    | O        | No               | Total outstanding/owed amount for credit card (the last known value)                                                                                                                                         | Fields value, precision, currency                                                                                                                                    |
| 1     | directDebitAccount       | ACCOUNTNO | O        | Yes              | Account number used for automated Direct Debit of credit card installment payment (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                   |                                                                                                                                                                      |
| 1     | directDebitSetup         | ENUM      | O        | Yes              | Setup for automated Direct Debit of credit card installment payment                                                                                                                                          | ENUM values: [MINIMAL_INSTALLMENT, OUTSTANDING_AMOUNT, NO_AUTOMATIC_REPAYMENT]                                                                                       |
| 1     | minimalMonthlyAmount     | AMOUNT    | O        | No               | Minimal installment repayment amount for credit card (at previous cycle end date)                                                                                                                            | Fields value, precision, currency                                                                                                                                    |
| 1     | outstandingMonthlyAmount | AMOUNT    | O        | No               | Total outstanding/owed amount for credit card (at previous cycle end date)                                                                                                                                   | Fields value, precision, currency                                                                                                                                    |
| 1     | installmentDueDate       | DATE      | O        | No               | Installment repayment due date for credit card                                                                                                                                                               | ISO date format:  YYYY-MM-DD                                                                                                                                         |
| 1     | repaymentRate            | FLOAT     | O        | No               | Repayment rate for credit card in %                                                                                                                                                                          |                                                                                                                                                                      |
| 1     | overLimitAmount          | AMOUNT    | O        | No               | Outstanding/owed amount which is over the limit of credit card                                                                                                                                               | Fields value, precision, currency                                                                                                                                    |
| 1     | overDueAmount            | AMOUNT    | O        | No               | Outstanding/owed amount which is over due date for credit card                                                                                                                                               | Fields value, precision, currency                                                                                                                                    |
| 1     | overDueDays              | INTEGER   | O        | No               | Number of days after due date for Outstanding/owed over due amount of credit card                                                                                                                            |                                                                                                                                                                      |
| 1     | mainAccount              | structure | O        | No               | Main account is credit card shadow account for credit card or linked main current account for bank/debit card                                                                                                |                                                                                                                                                                      |
| 2     | id                       | TEXT      | O        | No               | Internal ID as reference for account provided by BE                                                                                                                                                          |                                                                                                                                                                      |
| 2     | accountno                | ACCOUNTNO | M        | No               | Account number of main account (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)                                                                      |                                                                                                                                                                      |
| 2     | holderName               | TEXT      | M        | No               | Full name of the main account's holder.                                                                                                                                                                      | format "FirstName MiddleName LastName"                                                                                                                               |
| 1     | state                    | ENUM      | M        | No               | Current state of card (locked state is used for permanently blocked card)                                                                                                                                    | ENUM values: [ACTIVE, INACTIVE, LOCKED, TEMPORARY_BLOCKED, CLOSED] - `INACTIVE` (issued card not activated yet), `LOCKED` (permanently blocked), `TEMPORARY_BLOCKED` (could be unblocked to be active), `CLOSED` (expired, canceled) |
| 1     | type                     | ENUM      | M        | No               | Type of card: credit, debit/bankcard, indoor (Profit-Cards, Bonus-Cards special for AT)                                                                                                                      | ENUM values: [BANK_CARD, INDOOR, CREDIT] - `BANK_CARD` (used for debit card too)                                                                                     |
| 1     | provider                 | ENUM      | M        | No               | Credit card provider/issuer: Erste Bank or external bank                                                                                                                                                     | ENUM values: [ERSTE_BANK, EXTERNAL]                                                                                                                                  |
| 1     | lockReason               | ENUM      | O        | No               | Indicates reason for locking the card                                                                                                                                                                        | ENUM values: [THEFT, DELINQUENCY, LOSS, FRAUD, NEVER_RECEIVED, SOLVENCY, OTHER]                                                                                      |
| 1     | invoiceDeliveryMode      | ENUM      | O        | No (maybe later) | Indicates how credit card invoices are delivered                                                                                                                                                             | ENUM values: [POSTAL, ELECTRONICALLY]                                                                                                                                |
| 1     | cardDeliveryMode         | ENUM      | O        | No (maybe later) | Indicates how a client receives their card and pin                                                                                                                                                           | ENUM values: [BRANCH, HOME, BRANCH_PROLONGATION, OTHER_BRANCH, ADDRESS_ABROAD]                                                                                       |
| 1     | characteristic           | ENUM      | O        | No               | For ErsteBank cards, signifies what characteristic it has                                                                                                                                                    | ENUM values: [MAIN, AUTHORIZED, AUTHORIZED_WITH_STANDALONE_DEBIT_ACCOUNT, BUSINESS] - values could be country specific                                               |
| 1     | sequenceNumber           | TEXT      | M        | No               | Card sequence number. The number distinguishing between separate cards (different plastic cards) with the same Primary Account Number (PAN)                                                                  |                                                                                                                                                                      |
| 1     | insurance                | structure | O        | No               | Optional structure for insurance policy agreed for particular card                                                                                                                                           |                                                                                                                                                                      |
| 2     | type                     | TEXT      | M        | No               | Localized name or description of insurance policy type                                                                                                                                                       |                                                                                                                                                                      |
| 2     | insured                  | TEXT      | O        | No               | Full name of insured person(s) or organization.                                                                                                                                                              | format "FirstName MiddleName LastName"                                                                                                                               |
| 2     | validToDate              | DATE      | O        | No               | Insurance policy validity date                                                                                                                                                                               | ISO date format:  YYYY-MM-DD                                                                                                                                         |
| 1     | features                 | FEATURES  | O        | No               | Array of optional features valid for given card.                                                                                                                                                             | Features values - see table below                                                                                                                                    |
| 1     | flags                    | FLAGS     | O        | Yes              | Array of optional Flag values depends on Card type.                                                                                                                                                          | Flags values - see table below                                                                                                                                       |

See possible **feature** values in following table:

Feature                         | Description
------------------------------- | -----------------------------------------------------
`contactless`                   | Indicates whether contactless technology is available for this card
`secureOnlineShopping`          | Indicates whether 3D secure online shopping is available for this card
`accountLinkage`                | Indicates whether linked account information would be available
`reissuePin`                    | Indicates whether reissuing a PIN is possible
`reprintPin`                    | Indicates whether reprint of existing PIN is possible
`reissueCard`                   | Indicates whether reissuing a card is possible
`onlineLocking`                 | Indicates whether a card can be locked online
`invoiceDelivery`               | Indicates whether invoice delivery for statements can be switched between postal and electronical.
`smsNotification`               | Indicates whether SMS notifications for this card can be set.
`watchDogs`                     | Indicates whether watch dogs for this card can be set.
`revolving`                     | Indicates that this card could be switched to REVOLVING mode.
`limitChange`                   | Indicates that card limits could be changed for this card.
`temporaryLimitChange`          | Indicates that card temporary limits could be changed for this card.
`authorizedCards`               | Indicates that authorized (supplementary) cards exist for this card.
`temporarilyDeactivation`       | Indicates that several functions could be temp. deactivated for this (EB credit) card.
`replacementCard`               | Indicates that a replacement card could be ordered for this (bank) card.
`availableAmount`               | Indicates that the available to spend amount for a EB credit card can be increased by transferring money from a current account.
`cardDelivery`                  | Indicates whether card delivery mode could be changed for this card (not used in AT so far)
`automaticRepayment`            | Indicates whether automatic repayment (setup for Direct Debit) of credit card is possible

The following **flags** values exist:

Flag                            | Description
------------------------------- | -----------------------------------------------
`smsNotificationActive`         | Indicates that SMS notification is active for this card
`secureOnlineShoppingEnabled`   | Indicates that 3D secure online shopping functionality is active for this card
`contactlessEnabled`            | Indicates whether contactless functionality is active for given card
`automaticReplacementOn`        | Indicates whether automatic card replacement is active for given card
`businessCardUsage`             | Indicates whether - for an internal credit card that is a business card - the card is used as a business card
`revolving`                     | Indicates that credit card has revolving mode
`electronicStatementAllowed`    | User may see the electronic statements list and download statement for credit card (shadow account). (Flag is not used in AT, there are used invoices for CC)
`automaticRepaymentActive`      | Indicates that  automatic repayment (setup for Direct Debit) of credit card is active for this card

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "id": "CB5E22A9B81C6E04",
                "alias": "my gold card",
                "owner": "ELISABETH RICHTER",
                "number": "422093XXXXXX2416",
                "productCode": "CC_VISA_xyz"
                "productI18N": "Visa Card Gold",
                "expiryDate": "2017-06-30",
                "validFromDate": "2014-06-11",
                "limit": {
                    "value": 300000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "outstandingAmount": {
                    "value": 98750,
                    "precision": 2,
                    "currency": "EUR"
                },
                "directDebitAccount": {
                    "iban": "AT662011100001237957"
                },
                "minimalMonthlyAmount": {   
                    "value": 5150,
                    "precision": 2,
                    "currency": "EUR"
                },
                "outstandingMonthlyAmount": {
                    "value": 103000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "installmentDueDate": "2014-12-11",
                "repaymentRate": 18.9,
                "mainAccount": {
                    "id": "CD3FB47FE625310C",
                    "accountno": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    },
                    "holderName": "Dkffr. Manfred Dichtl"
                },
                "state": "LOCKED",
                "type": "CREDIT",
                "provider": "ERSTE_BANK",
                "lockReason": "THEFT",
                "invoiceDeliveryMode": "POSTAL",
                "cardDeliveryMode": "HOME",
                "characteristic": "MAIN",
                "sequenceNumber": "1",
                "features": [
                    "contactless",
                    "secureOnlineShopping",
                    "invoiceDelivery",
                    "reissueCard",
                    "reissuePin",
                    "smsNotification",
                    "watchDogs",
                    "revolving",
                    "replacementCard",
                    "authorizedCards"
                ],
                "flags": [
                    "smsNotificationActive",
                    "secureOnlineShoppingEnabled",
                    "contactlessEnabled",
                    "automaticReplacementOn"
                ]
            }

### Get a one single card [GET]
Returns the information about one specific card of user based on given ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
**Card** resource containing details of one user card identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Card][]

### Update single card [PUT]
Allows to change a limited set of card-settings of one specific card. 
Previously only the field *alias* could be changed, but now also automatic repayment (setup for Direct debit) and flag for electronic statement could be changed for credit card. Change only to *alias* field must not be signed, but automatic repayment and flag change must follow the signing process.
Even though other (not editable) fields are not stored they must fulfill the validation criteria of CARD-Object. *Id* in URL, *id* field and *number* field in payload: These fields must refer to the same card, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**Card** resource with updated *alias* attribute or changed direct debit setup or electronic statement flag for credit card.

#### Reply
A **Card** resource with updated details of one user card identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | card                    | Card        | M        | Card object                                            |                                                                |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope            | Purpose
-----------------|------------------|------------------------------------
`ID_NOT_FOUND`   | id               | The provided ID does not exist.
`ID_MISMATCH`    | id               | The given ID in payload doesn’t match to the ID in URI.
`FIELD_TOO_LONG` | alias            | Length of the provided alias is greater than 60.
`VALUE_INVALID`  | directDebitSetup | Invalid value, only values from ENUM list for direct debit (automatic repayment) are valid.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "CB5E22A9B81C6E04",
                "alias": "my flexible GOLD friend",
                "owner": "ELISABETH RICHTER",
                "number": "422093XXXXXX2416",
                "productI18N": "Visa Card Gold",
                "expiryDate": "2017-06-30",
                "directDebitAccount": {
                    "iban": "AT662011100001237957"
                },
                "state": "LOCKED",
                "type": "CREDIT",
                "provider": "ERSTE_BANK",
                "invoiceDeliveryMode": "POSTAL",
                "cardDeliveryMode": "HOME",
                "characteristic": "MAIN",
                "sequenceNumber": "1",
                "features": [
                    "contactless",
                    "secureOnlineShopping",
                    "invoiceDelivery",
                    "reissueCard",
                    "reissuePin",
                    "smsNotification",
                    "watchDogs",
                    "revolving",
                    "replacementCard",
                    "authorizedCards"
                ],
                "flags": [
                    "smsNotificationActive",
                    "secureOnlineShoppingEnabled",
                    "contactlessEnabled",
                    "automaticReplacementOn"
                ]
            }

+ Response 200 (application/json)

    + Body

            {
                "card": {
                    "id": "CB5E22A9B81C6E04",
                    "alias": "my flexible GOLD friend",
                    "owner": "ELISABETH RICHTER",
                    "number": "422093XXXXXX2416",
                    "productCode": "CC_VISA_xyz"
                    "productI18N": "Visa Card Gold",
                    "expiryDate": "2017-06-30",
                    "validFromDate": "2014-06-11",
                    "limit": {
                        "value": 300000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "outstandingAmount": {
                        "value": 98750,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "directDebitAccount": {
                        "iban": "AT662011100001237957"
                    },
                    "minimalMonthlyAmount": {   
                        "value": 5150,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "outstandingMonthlyAmount": {
                        "value": 103000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "installmentDueDate": "2014-12-11",
                    "repaymentRate": 18.9,
                    "mainAccount": {
                        "id": "CD3FB47FE625310C",
                        "accountno": {
                            "iban": "AT482011100000005702",
                            "bic": "GIBAATWWXXX"
                        },
                        "holderName": "Dkffr. Manfred Dichtl"
                    },
                    "state": "LOCKED",
                    "type": "CREDIT",
                    "provider": "ERSTE_BANK",
                    "lockReason": "THEFT",
                    "invoiceDeliveryMode": "POSTAL",
                    "cardDeliveryMode": "HOME",
                    "characteristic": "MAIN",
                    "sequenceNumber": "1",
                    "features": [
                        "contactless",
                        "secureOnlineShopping",
                        "invoiceDelivery",
                        "reissueCard",
                        "reissuePin",
                        "smsNotification",
                        "watchDogs",
                        "revolving",
                        "replacementCard",
                        "authorizedCards"
                    ],
                    "flags": [
                        "smsNotificationActive",
                        "secureOnlineShoppingEnabled",
                        "contactlessEnabled",
                        "automaticReplacementOn"
                    ]
                },
                "signInfo": {
                    "state": "NONE"
                }
            }


## CardList [/netbanking/my/cards{?size,page,sort,order,feature,featureSet,platformKey,noCache}]
Resource Card List represents collection of cards to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **Card** type items.

Description of **CardList** resource/type attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | cards          | ARRAY of Card    | O        | Array of cards accessible by the user (could be empty)               |                          |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are: `id` and `productCode`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.
    + feature (TEXT, optional) ... Optional multi valued features. It is possible to filter for certain product by using the feature URI parameter optional comma-separated list of features.
    + featureSet (TEXT, optional) ... Optional Feature sets are to be used as handy shortcuts for certain set of features. If no featureSet is supplied, `NONE` is assumed as default.
    + platformKey (TEXT, optional) ... Optional PlatformKey. If set, will return image ids for each card if appropriate mappings exist with the same platform key.
    + noCache (TEXT, optional) ... Optional noCache. If set to `true`, will not use any cached values for this request (optional URI parameter, default is `false`).

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 20,
                "cards": [
                    {
                        "id": "CB5E22A9B81C6E04",
                        "alias": "my gold card",
                        "owner": "ELISABETH RICHTER",
                        "number": "422093XXXXXX2416",
                        "productCode": "CC_VISA_xyz"
                        "productI18N": "Visa Card Gold",
                        "expiryDate": "2017-06-30",
                        "validFromDate": "2014-06-11",
                        "limit": {
                            "value": 300000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "outstandingAmount": {
                            "value": 98750,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "directDebitAccount": {
                            "iban": "AT662011100001237957"
                        },
                        "minimalMonthlyAmount": {   
                            "value": 5150,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "outstandingMonthlyAmount": {
                            "value": 103000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "installmentDueDate": "2014-12-11",
                        "repaymentRate": 18.9,
                        "mainAccount": {
                            "id": "CD3FB47FE625310C",
                            "accountno": {
                                "iban": "AT482011100000005702",
                                "bic": "GIBAATWWXXX"
                            },
                            "holderName": "Dkffr. Manfred Dichtl"
                        },
                        "state": "LOCKED",
                        "type": "CREDIT",
                        "provider": "ERSTE_BANK",
                        "lockReason": "THEFT",
                        "invoiceDeliveryMode": "POSTAL",
                        "cardDeliveryMode": "HOME",
                        "characteristic": "MAIN",
                        "sequenceNumber": "1",
                        "features": [
                            "contactless",
                            "secureOnlineShopping",
                            "invoiceDelivery",
                            "reissueCard",
                            "reissuePin",
                            "smsNotification",
                            "watchDogs",
                            "revolving",
                            "replacementCard",
                            "authorizedCards"
                        ],
                        "flags": [
                            "smsNotificationActive",
                            "secureOnlineShoppingEnabled",
                            "contactlessEnabled",
                            "automaticReplacementOn"
                        ]
                    },
                    {
                        "id": "C83AD63C3528640C",
                        "owner": "ELISABETH RICHTER",
                        "number": "01******1234",
                        "productI18N": "Botenkarte",
                        "limit": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "state": "LOCKED",
                        "type": "INDOOR",
                        "provider": "ERSTE_BANK",
                        "lockReason": "LOSS",
                        "flags": [
                        ]
                    }
                ]
            }

### Get a list of all cards for current user [GET]
Get possibly empty list of all cards, credit cards and debit/bank cards. This call is paginated and can be sorted by `id` and `productCode`.
This call delivers all cards in all states. It is possible to request/restrict additional information provided as output by using input *features* or *featureSet* URI parameter.

See possible **feature** and **featureSet** values in following tables:

Feature code                 | Description
---------------------------- | -----------------------------------------------------
`CARD_LIMIT_CHANGE`          | Indicates that (additional) information about the support for the limit change function for a EB card should be delivered.
`ACCOUNT_LINKAGE`            | Indicates that (additional) information about linked accounts for a bank card should be delivered.
`AUTHORIZED_CARDS`           | Indicates that (additional) information about the availability of authorized cards for a EB credit card should be delivered.
`AVAILABLE_AMOUNT`           | Indicates that (additional) information about available amounts for a internal credit should be delivered.

Local countries can use specific features, which will need George FE customization.

FeatureSet code              | Description
---------------------------- | -----------------------------------------------------
`NONE`                       | Indicates that no feature should be used.
`ALL`                        | Indicates that all available features should be used.
`ALL_NO_BALANCES`            | Indicates that all available features should be used but calls to getting balances.

When platformKey is specified in request URL, it makes imageData field visible in response. *platformKey* available values are: `IOS-640`, `IOS-750`, `IOS-1242`, `ANDROID-320`, `ANDROID-480`, `ANDROID-800`, `ANDROID-1080`, `ANDROID-1440`.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **CardList** with possibly empty (omitted) array of *embedded* **Card** items.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CardList][]


## CreditCardTransactionInfo [/netbanking/my/cards/{id}/transactions/{tId}]
Credit Card Transaction Info resource represents one single note, tag, star info assigned to transaction (identified by TID) executed using credit card identified by card ID. This transaction note, tag, star refer to single credit card transaction history line and user who entered/modified this info.

Description of **CreditCardTransactionInfo** resource attributes: 

| Level | Attribute name     | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                    | Expected values/format         |
|-------|--------------------|-----------------|----------|----------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| 1     | id                 | ID              | O        | No       | Internal identifier of credit card transaction provided by BE                                                            |                                |
| 1     | note               | TEXT140         | O        | Yes      | Personal, user specific note to card transaction, which could be added to transaction via FE and stored in BE (API)      |                                |
| 1     | flags              | FLAGS           | O        | Yes      | Array of optional Flag values, if not present then flag value is considered as false.                                    | FLAGS: `hasNote`, `hasStar`    |

+ Parameters
    + id (TEXT, required) ... ID internal identifier of card used as part of URI.
    + tId (TEXT, required) ... internal transaction identifier used as part of URI.

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    + Body

            {
                "id": "CCAAC0E017B6630CUTC20140204230000000",
                "note": "New client personal comment for card transaction",
                "flags": [
                    "hasNote", "hasStar"
                ]
            }

### Add/Change note and mark single card transaction [PUT]
Allows to add or change a client's personal note and mark/star the card transaction as favorite for one specific transaction on selected card. The existing note will be removed, if the given payload has an empty or missing *note* attribute. If *hasStar* flag is provided in input payload, card transaction is marked as favorite, otherwise the existing flag will be removed.
Transaction note and star are assigned to particular transaction and user who created/modified this transaction info. If two users have access to the same transaction (card), then they can define different personal notes to the same transaction and these personal notes should be visible only to related user. 

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

JSON payload in request consists of **CreditCardTransactionInfo** resource. 

#### Reply
Reply will consists of card transaction info structure **CreditCardTransactionInfo** identified by parameter TID on card identified by ID and related to user.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum                   | Mand/Opt | Attribute description                   | Expected values/format                                         |
|-------|-------------------------|-----------------------------|----------|-----------------------------------------|----------------------------------------------------------------|
| 1     | cardTransaction         | CreditCardTransactionInfo   | M        | CreditCardTransactionInfo object        |                                                                |
| 1     | signInfo                | SIGNINFO                    | M        | SignInfo Details                        |                                                                |

#### Error codes
Error code          | Scope          | Purpose
--------------------|----------------|------------------------------------
`ID_NOT_FOUND`      | id (URI)       | The provided card ID does not exist.
`ID_NOT_FOUND`      | tId            | The provided transaction TID does not exist.
`ID_MISMATCH`       | tId (URI), id  | The provided transaction TID (URI) differs from the transaction ID in payload.
`INVALID_CARD_TYPE` | id (URI)       | The selected card is not credit card.
`FIELD_TOO_LONG`    | note           | Length of the provided note is greater than 140.
`NOT_POSSIBLE`      | note           | A host failure occurred during create/update/delete a note.
`NOT_POSSIBLE`      | flags          | A host failure occurred during adding/removing a hasStar flag.

+ Request (application/json)

    [CreditCardTransactionInfo][]

+ Response 200 (application/json)

    + Body

            {
                "cardTransaction": {
                    "id": "CCAAC0E017B6630CUTC20140204230000000",
                    "note": "New client's personal comment for card transaction",
                    "flags": [
                        "hasVoucher", "hasStar"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "044971701790000016001245"
                }
            }


## CreditCardInvoiceTransaction [/netbanking/my/cards/{id}/invoices/{iId}/transactions/{tId}]

Credit Card Invoice Transaction resource represents one single transaction (identified by TID) executed using credit card identified by card ID. This transaction refers to single card invoice line.

Description of all possible **CreditCardInvoiceTransaction** resource attributes: 

| Level | Attribute name     | Type/Enum       | Mand/Opt | Editable | Attribute description                                                                                                                                                                | Expected values/format                                |
|-------|--------------------|-----------------|----------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| 1     | id                 | ID              | M        | No       | Internal identifier of credit card transaction provided by BE                                                                                                                        |                                                       |
| 1     | maskedCardNumber   | TEXT            | M        | No       | Masked card number (PAN - primary account number), only the first 6 and the last 4 digits are displayed, asterisk is used for the rest of digits.                                    | ISO 7812 format: "525405******1234"                   |
| 1     | referenceId        | TEXT            | M        | No       | Transaction reference ID (MBU Ersterfassungsreferenz) provided by BE (or Card Management System)                                                                                     |                                                       | 
| 1     | personalCardNumber | INTEGER         | O        | No       | Personal card number.                                                                                                                                                                | 2 digits (sCreditCard), 3 digits (cardcomplete) in AT |
| 1     | amount             | AMOUNT          | M        | No       | Booked amount on credit card account in account currency, value with minus if debit on card account (embedded AMOUNT type)                                                           |                                                       |
| 1     | amountSender       | AMOUNT          | O        | No       | Original card transaction amount in defined currency and with precision (embedded AMOUNT type). Must be provided when transaction currency and card account currency are different.  |                                                       |
| 1     | exchangeRate       | TEXT            | O        | No       | Optional information about exchange rates used for card transaction booking.                                                                                                         |                                                       |
| 1     | manipulationFee    | AMOUNT          | O        | No       | Optional transaction fee amount.                                                                                                                                                     |                                                       |
| 1     | date               | DATETIME        | M        | No       | Card Transaction date, when credit card was processed in ATM, POS, internet.                                                                                                         | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ            |
| 1     | bookingDate        | DATETIME        | M        | No       | Booking/accounting date in BE system in the moment of booking of card transaction                                                                                                    | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ            |
| 1     | additionalTexts    | ARRAY of TEXT   | O        | No       | Array of additional text fields, max 4x 35 characters. Card transaction description, message for receiver.                                                                           |                                                       |
| 1     | note               | TEXT140         | O        | Yes      | Personal, user specific note to card transaction, which could be added to transaction via FE and stored in BE (API)                                                                  |                                                       |
| 1     | mccCode            | TEXT            | O        | No       | MCC identification code of merchant category.                                                                                                                                        |                                                       |
| 1     | merchantName       | TEXT            | O        | No       | Merchant name assigned to credit card transaction.                                                                                                                                   |                                                       |
| 1     | location           | TEXT            | O        | No       | Optional information about location (merchant POS, bank ATM, internet) where card transaction was executed.                                                                           |                                                       |
| 1     | state              | ENUM            | M        | No       | State of transaction presented to user on FE                                                                                                                                         | ENUM values: [CLOSED, REJECTED]                       |
| 1     | characteristic     | ENUM            | M        | No       | Indicates whether this transaction has been issued by the main card or an authorized card.                                                                                           | ENUM values: [MAIN, AUTHORIZED]                       |
| 1     | flags              | FLAGS           | O        | Yes (hasStar only) | Array of optional Flag values, if not present then flag value is considered as false.                                                                                      | FLAGS: `hasNote`, `hasStar`, `isStorno`               |

+ Parameters
    + id (TEXT, required) ... ID internal identifier of card used as part of URI.
    + iId (TEXT, required) ... internal identifier of credit card invoice used as part of URI. This should be URI-Encoded, as this might contain a slash in URI path.
    + tId (TEXT, required) ... internal transaction identifier used as part of URI.

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "CCAAC0E017B6630CUTC20140204230000000",
                "maskedCardNumber": "525405******1234",
                "referenceId": "201111402192AB3-DA2007000031",
                "personalCardNumber": "19",
                "amount": {
                    "value": -2500,
                    "precision": 2,
                    "currency": "EUR"
                },
                "amountSender": {
                    "value": -3000,
                    "precision": 2,
                    "currency": "USD"
                },
                "exchangeRate": "1.2",
                "manipulationFee": {
                    "value": -10,
                    "precision": 2,
                    "currency": "EUR"
                },
                "date": "2014-02-03T14:21:00Z",
                "bookingDate": "2014-02-04T23:00:00Z",
                "additionalTexts": [
                    "Card online payment"
                ],
                "note": "my special note",
                "mccCode": "5732",
                "merchantName": "eBAY",
                "state": "CLOSED",
                "characteristic": "MAIN",
                "flags": [
                    "hasNote", "hasStar"
                ]
            }

### Add/Change note and mark single card transaction [PUT]
Allows to add or change a client's personal note and mark the card transaction as favorite for one specific transaction on selected card. The existing note will be removed, if the given payload has an empty or missing *note* attribute. If *hasStar* flag is provided in input payload, card transaction is marked as favorite, otherwise the existing flag will be removed. 
Even though other fields are not stored they must fulfill the validation criteria of **CreditCardInvoiceTransaction** Object.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

JSON payload in request consists of entire **CreditCardInvoiceTransaction** resource. 

#### Reply
**CreditCardInvoiceTransaction** resource containing details of one card transaction identified by parameter TID on card identified by ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum                        | Mand/Opt | Attribute description                      | Expected values/format                                     |
|-------|-------------------------|----------------------------------|----------|--------------------------------------------|------------------------------------------------------------|
| 1     | cardTransaction         | CreditCardInvoiceTransaction     | M        | CreditCardInvoiceTransaction object        |                                                            |
| 1     | signInfo                | SIGNINFO                         | M        | SignInfo Details                           |                                                            |

#### Error codes
Error code          | Scope          | Purpose
--------------------|----------------|------------------------------------
`ID_NOT_FOUND`      | id (URI)       | The provided card ID does not exist.
`ID_NOT_FOUND`      | iId            | The provided invoice IID does not exist.
`ID_NOT_FOUND`      | tId            | The provided transaction TID does not exist.
`ID_MISMATCH`       | tId (URI), id  | The provided transaction TID (URI) differs from the transaction ID in payload.
`INVALID_CARD_TYPE` | id (URI)       | The selected card is not credit card.
`VALUE_INVALID`     | appId          | The resource is only available for george and quickcheck clients.
`FIELD_TOO_LONG`    | note           | Length of the provided note is greater than 140.
`NOT_POSSIBLE`      | note           | A host failure occurred during create/update/delete a note.
`NOT_POSSIBLE`      | flags          | A host failure occurred during adding/removing a hasStar flag.

+ Request (application/json)

    [CreditCardInvoiceTransaction][]
        
+ Response 200 (application/json)

    + Body

            {
                "cardTransaction": {
                    "id": "CCAAC0E017B6630CUTC20140204230000000",
                    "maskedCardNumber": "525405******1234",
                    "referenceId": "201111402192AB3-DA2007000031",
                    "personalCardNumber": "19",
                    "amount": {
                        "value": -2500,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "amountSender": {
                        "value": -3000,
                        "precision": 2,
                        "currency": "USD"
                    },
                    "exchangeRate": "1.2",
                    "manipulationFee": {
                        "value": -10,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "date": "2014-02-03T14:21:00Z",
                    "bookingDate": "2014-02-04T23:00:00Z",
                    "additionalTexts": [
                        "Card online payment"
                    ],
                    "note": "my special note",
                    "mccCode": "5732",
                    "merchantName": "eBAY",
                    "state": "CLOSED",
                    "characteristic": "MAIN",
                    "flags": [
                        "hasNote", "hasStar"
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "045971701790000016001245"
                }
            }


## CardLimits [/netbanking/my/cards/{id}/card-limits]
Card Limits resource represents information about different limits for given user card.

Description of **CardLimits** resource attributes: 

| Level | Attribute name           | Type/Enum | Mand/Opt | Editable | Attribute description                                                                | Expected values/format                                     |
|-------|--------------------------|-----------|----------|----------|--------------------------------------------------------------------------------------|------------------------------------------------------------|
| 1     | limits                   | ARRAY of  | O        | No       | Array of defined card's limits (country specific)                                    |                                                            |
| 2     | limitType                | ENUM      | M        | No       | Limit type defines ATM, POS, internet/eCommerce, total limits                        | ENUM values: [ATM, POS, INTERNET, TOTAL]                   |
| 2     | limitPeriod              | ENUM      | M        | No       | Bank limit's period in days defined for limit type (default daily - 1D)              | ENUM values: [1D, 2D, 3D, 5D, 7D, 10D, 15D, 30D]           |
| 2     | limit                    | AMOUNT    | M        | Yes      | Current limit amount valid for limit's type and period                               |                                                            |
| 2     | temporaryLimit           | AMOUNT    | O        | Yes      | Temporary limit amount valid for limit's type and period                             |                                                            |
| 2     | bankLimit                | AMOUNT    | O        | No       | Maximum limit amount defined by bank valid for limit's type and period               |                                                            |
| 2     | temporaryLimitExpiration | DATETIME  | O        | Yes      | Temporary limit expiration date for limit's type and period                          | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                 |
| 1     | confirmations            | ARRAY of  | O        | Yes      | Confirmation structure (possible collection), where automatic confirmation of action (only update) could be sent |                                |
| 2     | contactId                | TEXT      | O        | Yes      | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)  |                                |
| 2     | email                    | EMAIL     | M        | Yes      | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                              | E.g. "john.doe@test.com"       |
| 2     | language                 | ENUM      | M        | Yes      | Predefined language which should be used for confirmation template.                                              | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "limits": [
                    {
                        "limitType": "ATM",
                        "limitPeriod": "5D",
                        "limit": {
                            "value": 1500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimit": {
                            "value": 300000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 7000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimitExpiration": "2014-12-12T22:21:00Z"
                    },
                    {
                        "limitType": "POS",
                        "limitPeriod": "1D",
                        "limit": {
                            "value": 3000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimit": {
                            "value": 500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimitExpiration": "2014-12-15T22:00:00Z"
                    },
                    {
                        "limitType": "INTERNET",
                        "limitPeriod": "2D",
                        "limit": {
                            "value": 1000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimit": {
                            "value": 200000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 4500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimitExpiration": "2014-12-15T22:00:00Z"
                    },
                    {
                        "limitType": "TOTAL",
                        "limitPeriod": "1D",
                        "limit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    }
                ]
            }

### Get limits of card [GET]
Returns list of defined limits for given card ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **CardLimits** with card's limits information.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CardLimits][]
    
### Update card limits [PUT]
This endpoint is used to update card limits of card identified by ID. The resource is a signable resource. To apply the changes to the actual card limits the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**CardLimits** resource with updated limits and temporary limits requested by user.

#### Reply
**CardLimits** type containing updated card limits information of the particular card.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | limits                  | CardLimits  | M        | CardLimits object                                      |                                                                |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code          | Scope            | Purpose
--------------------|------------------|---------------------------
`ID_NOT_FOUND`      | id               | The provided ID does not exist.
`VALUE_INVALID`     | limitType        | Only `ATM`, `POS`, `INTERNET` and `TOTAL` values are valid.
`VALUE_INVALID`     | limitPeriod      | Only `1D`, `2D`, `3D`, `5D`, `7D`, `10D`, `15D`, `30D` values are valid.
`FIELD_EMPTY`       | limit            | Limit attribute is mandatory 

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "limits": [
                    {
                        "limitType": "ATM",
                        "limitPeriod": "5D",
                        "limit": {
                            "value": 1990000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    },
                    {
                        "limitType": "POS",
                        "limitPeriod": "1D",
                        "limit": {
                            "value": 3000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimit": {
                            "value": 1500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimitExpiration": "2015-05-15T22:00:00Z"
                    }
                ],
                "confirmations": [
                    {
                        "email": "tomas.sporitelni@csas.cz",
                        "language": "cs"
                    }
                ]
            }

+ Response 200 (application/json)

    + Body

            {
                "limits": [
                    {
                        "limitType": "ATM",
                        "limitPeriod": "5D",
                        "limit": {
                            "value": 1990000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 7000000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    },
                    {
                        "limitType": "POS",
                        "limitPeriod": "1D",
                        "limit": {
                            "value": 3000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimit": {
                            "value": 1500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "temporaryLimitExpiration": "2015-05-15T22:00:00Z"
                    },
                    {
                        "limitType": "INTERNET",
                        "limitPeriod": "2D",
                        "limit": {
                            "value": 1000000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 4500000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    },
                    {
                        "limitType": "TOTAL",
                        "limitPeriod": "1D",
                        "limit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        },
                        "bankLimit": {
                            "value": 3500000,
                            "precision": 2,
                            "currency": "CZK"
                        }
                    }
                ],
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016009991"
                }
            }


## LinkedAccounts [/netbanking/my/cards/{id}/accounts]
Linked Accounts resource represents all linked accounts to given card identified by ID.

Description of **LinkedAccounts** resource attributes: 

| Level | Attribute name          | Type/Enum | Mand/Opt | Attribute description                            | Expected values/format         |
|-------|-------------------------|-----------|----------|--------------------------------------------------|--------------------------------|
| 1     | linkedAccounts          | ARRAY of  | O        | Array of linked accounts                         |                                |
| 2     | accountId               | TEXT      | M        | Internal account ID used in BE systems           |                                |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "linkedAccounts": [
                    {
                        "accountId": "CB74FED2EA28E114"
                    },
                    {
                        "accountId": "CB03B65B59A2C104"
                    }
                ]
            }   

### Get a list of linked accounts for card [GET]
Returns all linked accounts for provided cardID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **LinkedAccounts** with list of linked account ID.

#### Error codes
Error code               | Scope    | Purpose
-------------------------|----------|------------------------------------
`ID_NOT_FOUND`           | id       | The provided ID does not exist.
`FEATURE_NOT_SUPPORTED`  | id       | Card does not support account linkage feature  

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [LinkedAccounts][]


## AuthorizedCards [/netbanking/my/cards/{id}/authorized-cards]
Authorized Cards resource represents list of authorized/supplementary cards linked to given main card identified by ID.

Description of **AuthorizedCards** resource attributes: 

| Level | Attribute name          | Type/Enum | Mand/Opt | Attribute description                                                        | Expected values/format                 |
|-------|-------------------------|-----------|----------|------------------------------------------------------------------------------|----------------------------------------|
| 1     | authorizedCards         | ARRAY of  | O        | Array of authorized cards                                                    |                                        |
| 2     | id                      | TEXT      | M        | Internal card ID used in BE systems                                          |                                        |
| 2     | holderName              | TEXT      | M        | Full name of the supplementary card holder.                                  | format "FirstName MiddleName LastName" |
| 2     | maskedPan               | TEXT      | M        | Masked card number (PAN-primary account number)                              | ISO 7812 format: "525405******1234"    |
| 2     | state                   | ENUM      | M        | Current state of card (locked state is used for permanently blocked card)    | ENUM values: [ACTIVE, INACTIVE, LOCKED, TEMPORARY_BLOCKED, CLOSED] -`INACTIVE` (issued card not activated yet), `LOCKED` (permanently blocked), `TEMPORARY_BLOCKED` (could be unblocked to be active), `CLOSED` (expired, canceled) |
| 2     | lockReason              | ENUM      | O        | Indicates reason for locking the card                                        | ENUM values: [THEFT, DELINQUENCY, LOSS, FRAUD, SOLVENCY, OTHER] - `NEVER_RECEIVED` (by Post delivery) |
| 2     | characteristic          | ENUM      | M        | For ErsteBank cards, signifies what characteristic it has                    | ENUM values: [AUTHORIZED, AUTHORIZED_WITH_STANDALONE_DEBIT_ACCOUNT] |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "authorizedCards": [
                    {
                        "id": "CD9930C7762DDA0C",
                        "holderName": "DDR. TESTER KRAJINOVIC",
                        "maskedPan": "544990XXXXXX9420",
                        "state": "ACTIVE",
                        "characteristic": "AUTHORIZED"
                    },
                    {
                        "id": "CD9930ECD968F70C",
                        "holderName": "AAA MATTHIAS CHRIS",
                        "maskedPan": "544990XXXXXX9438",
                        "state": "LOCKED",
                        "lockReason": "THEFT",
                        "characteristic": "AUTHORIZED"
                    }
                ]
            }

### Get a list of authorized cards for main card [GET]
Returns all linked authorized cards for a given card identified by ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **AuthorizedCards** with list of authorized cards for a given card.

#### Error codes
Error code               | Scope    | Purpose
-------------------------|----------|------------------------------------
`ID_NOT_FOUND`           | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [AuthorizedCards][]


## Card3DSecure [/netbanking/my/cards/{id}/secure-online-shopping]
Card 3D Secure resource represents secure online shopping information for given user card.

Description of **Card3DSecure** resource attributes: 

| Level | Attribute name          | Type/Enum | Mand/Opt | Editable | Attribute description                                                             | Expected values/format            |
|-------|-------------------------|-----------|----------|----------|-----------------------------------------------------------------------------------|-----------------------------------|
| 1     | status                  | ENUM      | O        | No       | 3D secure functionality status                                                    | ENUM values: [OK, NOT_ACTIVATED]  |
| 1     | pam                     | TEXT30    | O        | Yes      | Personal Assurance Message (PAM) that user chose when activate 3D secure          |                                   |
| 1     | phoneNumber             | TEXT      | O        | Yes      | Phone number used for 3D Secure authentification (in AT it should be in Contacts) | Phone number should be masked     |
| 1     | language                | ENUM      | O        | Yes      | Predefined language (by cardholder) of authentication window displayed by card issuer bank during 3D secure flow. | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "status": "OK",
                "pam": "My 3D Secure Personal salutation",
                "phoneNumber": "+420*****1234",
                "language": "cs"
            }

### Get a 3D secure status of card [GET]
Returns the 3D secure online shopping status for a single card given it’s ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **Card3DSecure** with status information.

#### Error codes
Error code                              | Scope    | Purpose
----------------------------------------|----------|------------------------------------
`ID_NOT_FOUND`                          | id       | The provided ID does not exist.
`SECURE_ONLINE_SHOPPING_STATUS_FAILED`  | status   | The status query did not succeed.             
`CARD_LOCKED`                           | status   | Card is already locked; getting status is not possible  
`FEATURE_NOT_SUPPORTED`                 | id       | Card does not support secure online shopping feature  

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Card3DSecure][]


## CardActions [/netbanking/my/cards/{id}/states]
Card Actions resource represents various active operation actions requested on single card identified by ID.

Description of **CardActions** resource attributes: 

| Level | Attribute name        | Type/Enum | Mand/Opt | Attribute description                                                                                                  | Expected values/format         |
|-------|-----------------------|-----------|----------|------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| 1     | action                | ENUM      | M        | Enumeration of active operation actions available for card                                                             | ENUM values: [REISSUE_PIN, REPRINT_PIN, REISSUE_CARD, LOCK_CARD, ACTIVATE_CARD, SET_CONTACTLESS_ON, SET_CONTACTLESS_OFF, SET_AUTOMATIC_REPLACEMENT_ON, SET_AUTOMATIC_REPLACEMENT_OFF] |
| 1     | lockReason            | ENUM      | O        | Lock reason enumeration value. Available only for action `LOCK_CARD`                                                   | ENUM values: [THEFT, LOSS]     |
| 1     | replacementRequested  | BOOLEAN   | O        | Flag indicates if card replacement is requested by user during card blocking. Available only for action `LOCK_CARD`    | Boolean values: `true`/`false` |
| 1     | confirmations         | ARRAY of  | O        | Confirmation structure (possible collection), where automatic confirmation of action could be sent                     |                                |
| 2     | contactId             | TEXT      | O        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)        |                                |
| 2     | email                 | EMAIL50   | M        | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                    | E.g. "john.doe@test.com"       |
| 2     | language              | ENUM      | M        | Predefined language which should be used for confirmation template.                                                    | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "action": "LOCK_CARD",
                "lockReason": "THEFT",
                "confirmations": [
                    {
                        "email": "tomas.sporitelni@csas.cz",
                        "language": "cs"
                    }
                ]
            }

### Request card active operation [PUT]
This endpoint is used to issue various actions on a single card identified by ID. The resource is a signable resource. To apply the changes to the actual card the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**CardActions** resource with definition of action requested by user.

#### Reply
**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code              | Scope                 | Purpose
------------------------|-----------------------|---------------------------
`ID_NOT_FOUND`          | id                    | The provided ID does not exist.
`CARD_LOCKED`           | id                    | Card is already locked; issuing any card action is not possible
`CARD_CLOSED`           | id                    | Card is already closed; issuing any card action is not possible
`FEATURE_NOT_SUPPORTED` | id                    | Card does not support the requested state change features
`INVALID_LOCK_REASON`   | lockReason            | Invalid lock reason specified
`VALUE_INVALID`         | replacementRequested  | Invalid value, only boolean values 'true'/'false' are valid

+ Request

    [CardActions][]

+ Response 200 (application/json)

    + Body

            {
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000019999991"
                }
            }


## CardDelivery [/netbanking/my/cards/{id}/delivery]
Resource Card delivery represents card/PIN delivery mode and delivery address information.

Description of **CardDelivery** resource attributes: 

| Level | Attribute name     | Type/Enum | Mand/Opt | Editable  | Attribute description                                                                                                            | Expected values/format         |
|-------|--------------------|-----------|----------|-----------|----------------------------------------------------------------------------------------------------------------------------------|--------------------------------|
| 1     | cardDeliveryMode   | ENUM      | M        | Yes       | Indicates how a client receives their card and pin                                                                               | ENUM values: [BRANCH, HOME, BRANCH_PROLONGATION, OTHER_BRANCH, ADDRESS_ABROAD] |
| 1     | branchId           | TEXT      | C        | Yes       | ID identificator of branch where Card/PIN will be delivered. Mandatory for mode: `BRANCH`, `BRANCH_PROLONGATION`, `OTHER_BRANCH` |                                |
| 1     | contactId          | TEXT      | O        | No (yet)  | Contact ID for Card/PIN delivery address from existing user contacts. Optional for mode: `HOME`, `ADDRESS_ABROAD`                |                                |
| 1     | address            | structure | C        | No (yet)  | Card/PIN delivery Address structure. Mandatory for mode: `HOME`, `ADDRESS_ABROAD`, else optional (always provided for CZ)        |                                |
| 2     | description        | TEXT35    | O        | No (yet)  | Name and description of addressee (for card/PIN delivery)                                                                        |                                |
| 2     | street             | TEXT35    | M        | No (yet)  | Street name with optional abbreviation for street type or location name (for places with unnamed streets)                        |                                |
| 2     | streetNumber       | TEXT10    | O        | No (yet)  | Street number with optional abbreviation (orientation number related to the street in CZ and SK)                                 |                                |
| 2     | buildingApartment  | TEXT35    | O        | No (yet)  | Building name or building descriptive number unique within the town/village/location (in CZ, SK), floor and apartment number     |                                |
| 2     | zipCode            | TEXT10    | M        | No (yet)  | Postal ZIP Code                                                                                                                  |                                |
| 2     | city               | TEXT35    | M        | No (yet)  | City/village name with optional county or city district or region                                                                |                                |
| 2     | country            | ISO3166   | M        | No (yet)  | ISO 3166 ALPHA-2 code of country (e.g. AT)                                                                                       |                                |
| 1     | deliveryPassword   | TEXT      | O        | Yes       | Password used when card/PIN delivery is to address abroad                                                                        |                                |
| 1     | deliveryPhone      | TEXT      | O        | Yes       | Phone contact used when card/PIN delivery is to address abroad                                                                   |                                |
| 1     | confirmations      | ARRAY of  | O        | Yes       | Confirmation structure (possible collection), where automatic confirmation of action could be sent                               |                                |
| 2     | contactId          | TEXT      | O        | No        | E-mail Contact ID from existing user contacts which will be as default (main email contact or selected by user)                  |                                |
| 2     | email              | EMAIL50   | M        | Yes       | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50)                                                              | E.g. "john.doe@test.com"       |
| 2     | language           | ENUM      | M        | Yes       | Predefined language which should be used for confirmation template.                                                              | ISO 639-1 ENUM values: [en, de, cs, sk, hr, sr, ro, hu] |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Body

            {
                "cardDeliveryMode": "HOME",                     
                "address": {
                    "description": "Peter Lipka",
                    "street": "Kratka",
                    "streetNumber": "5A",
                    "zipCode": "11000",
                    "city": "Praha 1",
                    "country": "CZ"
                }
                "confirmations": [
                    {
                        "email": "tomas.sporitelni@csas.cz",
                        "language": "cs"
                    }
                ]
            }

### Retrieve a card delivery settings [GET]
Get card/PIN delivery settings for user card identified by ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **CardDelivery** resource containing Card/PIN delivery settings of user card.

#### Error codes
Error code              | Scope         | Purpose
------------------------|---------------|---------------------------
`ID_NOT_FOUND`          | id            | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CardDelivery][]

### Update a card delivery settings [PUT]
Change the card/PIN delivery mode and delivery branch or delivery address. The resource is a signable resource. To apply the changes to the actual user settings the signing-workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**CardDelivery** resource with updated attributes, currently only delivery mode, branch for delivery and password and phone for delivery to abroad can be updated. Changes on not editable fields are ignored (No error message is returned).

#### Reply
**CardDelivery** resource containing updated card/PIN delivery settings information of the card.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum    | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|--------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | cardDelivery            | CardDelivery | M        | CardDelivery object                                    |                                                                |
| 1     | signInfo                | SIGNINFO     | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code              | Scope             | Purpose
------------------------|-------------------|---------------------------
`ID_NOT_FOUND`          | id                | The provided ID does not exist.
`CARD_LOCKED`           | id                | Card is already locked; issuing any card action is not possible
`CARD_CLOSED`           | id                | Card is already closed; issuing any card action is not possible
`FEATURE_NOT_SUPPORTED` | id                | Card does not support the requested delivery change feature
`VALUE_INVALID`         | cardDeliveryMode  | Invalid value, only values from ENUM list are valid

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
    
    + Body

            {
                "cardDeliveryMode": "BRANCH",                     
                "branchId": "17",
                "confirmations": [
                    {
                        "email": "tomas.sporitelni@csas.cz",
                        "language": "cs"
                    }
                ]
            }

+ Response 200 (application/json)

    + Body

            {
                "cardDelivery": {
                    "cardDeliveryMode": "BRANCH",                     
                    "branchId": "17",
                    "confirmations": [
                        {
                            "email": "tomas.sporitelni@csas.cz",
                            "language": "cs"
                        }
                    ]
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016006666"
                }
            }


## CardTransfer [/netbanking/my/cards/{id}/transfer]
Card Transfer resource is used to define request for transfer money to/from main/shadow account of Credit Card identified by ID.
The following transfer types are available:

- `DEBT_REPAYMENT` - request of Repayment (pay off) of Credit Card debt from selected user's current account
- `LIMIT_INCREASE` - request of Credit Card limit increase from selected user's current account
- `QUICK_DROWDOWN` - request of Quick drowdown from Credit Card to any account defined by user (available for domestic payment)
    
Description of **CardTransfer** resource attributes: 

| Level | Attribute name  | Type/Enum | Mand/Opt | Attribute description                                                                                                                   | Expected values/format                                         |
|-------|-----------------|-----------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| 1     | type            | ENUM      | M        | Transfer type from predefined group of Credit card available transfers                                                                  | ENUM values: [DEBT_REPAYMENT, LIMIT_INCREASE, QUICK_DROWDOWN]  |
| 1     | receiver        | structure | C        | Receiver account structure of particular credit card transfer type, mandatory for QUICK_DROWDOWN                                        | E.g. account where to sent money from CC for QUICK_DROWDOWN; CC main account or internal bank account for DEBT_REPAYMENT/LIMIT_INCREASE |
| 2     | id              | TEXT      | O        | Internal ID as reference for account (provided by BE)                                                                                   |                                                                |
| 2     | accountno       | ACCOUNTNO | M        | Account number structure (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)       |                                                                |
| 1     | sender          | structure | C        | Sender account structure of particular credit card transfer type, mandatory for DEBT_REPAYMENT, LIMIT_INCREASE                          | E.g. account from where user wants to sent money to CC for DEBT_REPAYMENT/LIMIT_INCREASE; CC main account or internal bank account for QUICK_DROWDOWN |
| 2     | id              | TEXT      | O        | Internal ID as reference for account (provided by BE)                                                                                   |                                                                |
| 2     | accountno       | ACCOUNTNO | M        | Account number structure (embedded ACCOUNTNO type: IBAN with optional BIC or local account number with mandatory local bank code)       |                                                                |
| 1     | amount          | AMOUNT    | M        | Amount of Credit card transfer entered by user                                                                                          | Fields value, precision, currency                              |

+ Parameters
    + id (TEXT) ... ID internal identifier of card used as part of URI.

+ Model

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "type": "DEBT_REPAYMENT",
                "receiver": {
                    "id": "CD3FB47FE625310C",
                    "accountno": {
                        "iban": "AT482011100000005702",
                        "bic": "GIBAATWWXXX"
                    }
                },
                "sender": {
                    "id": "CD3FB47FE625DD3838",
                    "accountno": {
                        "iban": "AT642011120031692500",
                        "bic": "GIBAATWWXXX"
                    }
                },
                "amount": {
                    "value": 2900000,
                    "precision": 2,
                    "currency": "EUR"
                }
            }

### Request credit card transfer operation [PUT]
This endpoint is used to request transfer from/to single Credit Card identified by ID. 
Setup of sender and receiver account structures depends on selected transfer type from available types:

Transfer type    | Credit card transfer type description
-----------------|-------------------------------------------------------------------------------------------------
`DEBT_REPAYMENT` | Request of Repayment (pay off) of Credit Card debt from selected user's current account (sender)
`LIMIT_INCREASE` | Request of Credit Card limit increase from selected user's current account (sender)
`QUICK_DROWDOWN` | Request of Quick drowdown from Credit Card (sender - CC main account or bank internal account) to any account defined by user (receiver - account available for domestic payment, could be also in other bank)

The resource is a signable resource. To do Credit card transfer the signing workflow has to be finished successfully.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

**CardTransfer** resource with definition of account and repayment amount requested by user.

#### Reply
**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code              | Scope         | Purpose
------------------------|---------------|---------------------------
`ID_NOT_FOUND`          | id            | The provided ID does not exist.
`INVALID_CARD_ACTION`   | id            | Can’t perform this action on a given card.
`VALUE_INVALID`         | type          | Invalid value, only values from ENUM list for transfer type are valid.

+ Request

    [CardTransfer][]

+ Response 200 (application/json)

    + Body

            {
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000019999991"
                }
            }


# Group Insurances
Insurance products related resources of *Banking Services API*.

## Insurance [/netbanking/my/contracts/insurances/{id}]
Insurance resource represents user insurance product of different types (pension/life/fund/accident/property insurance).

Description of **Insurance** resource attributes: 

| Level | Attribute name             | Type/Enum | Mand/Opt   | Editable | Attribute description                                                                                                                                              | Expected values/format                                          |
|-------|----------------------------|-----------|------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| 1     | id                         | ID        | M          | No       | Internal ID as reference for insurance contract                                                                                                                    |                                                                 |
| 1     | type                       | ENUM      | M          | No       | Product Type of insurance (Pension/Life/Fund/Accident/Property)                                                                                                    | ENUM values: [PENSION, LIFE, FUND, ACCIDENT, PROPERTY]          |
| 1     | product                    | TEXT      | M          | No       | Insurance product name. Localization of expected values and George FE behavior is needed, because Erste Group doesn't use harmonized group product definitions.    | Local values: should be specified                               |
| 1     | productI18N                | TEXT      | M          | No       | Localized insurance product name depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language).     | Values e.g.: `Whole life insurance`, `Family accident insurance`|
| 1     | description                | TEXT      | O          | No       | Additional description of insurance product, additional charges, index applied to insurance contract                                                               |                                                                 |
| 1     | alias                      | TEXT60    | O          | Yes      | Insurance contract alias stored in BackEnd (could be reused in mobile app as well).                                                                                |                                                                 |
| 1     | insurancePolicyHolder      | TEXT      | M          | No       | The primary holder of the specific insurance contract                                                                                                              | format "FirstName MiddleName LastName"                          |
| 1     | policyNumber               | TEXT      | M          | No       | The insurance policy/contract number of the specific insurance contract                                                                                            |                                                                 |
| 1     | status                     | ENUM      | O          | No       | The status of the specific insurance contract                                                                                                                      | ENUM values: [PROPOSAL, ACTIVE, CLOSED]                         |
| 1     | pension                    | structure | O          | No       | Structure for Pension insurance products                                                                                                                           |                                                                 |
| 2     | premiumPaymentInterval     | ENUM      | M          | No       | Insurance Premium payment interval frequency                                                                                                                       | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | premium                    | AMOUNT    | M          | No       | Agreed insurance premium payment amount for Pension insurance (_embedded AMOUNT type)                                                                              | Fields value, precision, currency                               |
| 2     | retirementDate             | DATE      | M          | No       | Retirement date of contract holder                                                                                                                                 | ISO date format:  YYYY-MM-DD                                    |
| 2     | guaranteedPensionCapital   | AMOUNT    | M          | No       | Guaranteed pension capital amount value (_embedded AMOUNT type)                                                                                                    | Fields value, precision, currency                               |
| 2     | expectedPensionCapital     | AMOUNT    | O          | No       | Expected pension capital amount value (_embedded AMOUNT type)                                                                                                      | Fields value, precision, currency                               |
| 2     | pensionPayoffMethod        | ENUM      | M          | No       | Pension payoff method, time frequency                                                                                                                              | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | guaranteedPension          | AMOUNT    | M          | No       | Guaranteed pension amount value (_embedded AMOUNT type)                                                                                                            | Fields value, precision, currency                               |
| 2     | expectedPension            | AMOUNT    | O          | No       | Expected pension amount value (_embedded AMOUNT type)                                                                                                              | Fields value, precision, currency                               |
| 1     | life                       | structure | O          | No       | Structure for Life insurance products                                                                                                                              |                                                                 |
| 2     | premiumPaymentInterval     | ENUM      | M          | No       | Insurance Premium payment interval frequency                                                                                                                       | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | premium                    | AMOUNT    | M          | No       | Agreed insurance premium payment amount for Life insurance (_embedded AMOUNT type)                                                                                 | Fields value, precision, currency                               |
| 2     | contractStartDate          | DATE      | O          | No       | Agreed start date of insurance contract                                                                                                                            | ISO date format:  YYYY-MM-DD                                    |
| 2     | contractEndDate            | DATE      | M          | No       | Agreed end date of insurance contract                                                                                                                              | ISO date format:  YYYY-MM-DD                                    |
| 2     | insuredAmount              | AMOUNT    | M          | No       | Insured amount value in Life insurance contract (_embedded AMOUNT type)                                                                                            | Fields value, precision, currency                               |
| 2     | currentCapitalValue        | AMOUNT    | O          | No       | Current Capital amount value of Life insurance contract (_embedded AMOUNT type)                                                                                    | Fields value, precision, currency                               |
| 2     | expectedPayoffAmount       | AMOUNT    | O          | No       | Expected payoff amount value in the end of contract (_embedded AMOUNT type)                                                                                        | Fields value, precision, currency                               |
| 1     | fund                       | structure | O          | No       | Structure for Fund insurance products                                                                                                                              |                                                                 |
| 2     | premiumPaymentInterval     | ENUM      | M          | No       | Insurance Premium payment interval frequency                                                                                                                       | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | premium                    | AMOUNT    | M          | No       | Agreed insurance premium payment amount for Fund insurance (_embedded AMOUNT type)                                                                                 | Fields value, precision, currency                               |
| 2     | contractStartDate          | DATE      | O          | No       | Agreed start date of insurance contract                                                                                                                            | ISO date format:  YYYY-MM-DD                                    |
| 2     | contractEndDate            | DATE      | M          | No       | Agreed end date of insurance contract                                                                                                                              | ISO date format:  YYYY-MM-DD                                    |
| 2     | insuredAmount              | AMOUNT    | M          | No       | Insured amount value in Fund insurance contract (_embedded AMOUNT type)                                                                                            | Fields value, precision, currency                               |
| 1     | accident                   | structure | O          | No       | Structure for Accident insurance products                                                                                                                          |                                                                 |
| 2     | premiumPaymentInterval     | ENUM      | M          | No       | Insurance Premium payment interval frequency                                                                                                                       | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | premium                    | AMOUNT    | M          | No       | Agreed insurance total premium payment amount for Accident insurance (_embedded AMOUNT type)                                                                       | Fields value, precision, currency                               |
| 2     | contractStartDate          | DATE      | O          | No       | Agreed start date of insurance contract                                                                                                                            | ISO date format:  YYYY-MM-DD                                    |
| 2     | contractEndDate            | DATE      | M          | No       | Agreed end date of insurance contract                                                                                                                              | ISO date format:  YYYY-MM-DD                                    |
| 2     | permanentDisabilityInsured | AMOUNT    | M          | No       | Permanent Disability Insured amount value in Accident insurance contract (_embedded AMOUNT type)                                                                   | Fields value, precision, currency                               |
| 2     | accidentalDeathInsured     | AMOUNT    | M          | No       | Accidental Death Insured amount value in Accident insurance contract (_embedded AMOUNT type)                                                                       | Fields value, precision, currency                               |
| 1     | property                   | structure | O          | No       | Structure for Property insurance products                                                                                                                          |                                                                 |
| 2     | premiumPaymentInterval     | ENUM      | M          | No       | Insurance Premium payment interval frequency                                                                                                                       | ENUM values: [ONCE, MONTHLY, QUARTERLY, HALFYEARLY, YEARLY, UNKNOWN] |
| 2     | premium                    | AMOUNT    | M          | No       | Agreed insurance premium payment amount for Property insurance (_embedded AMOUNT type)                                                                             | Fields value, precision, currency                               |
| 2     | contractStartDate          | DATE      | O          | No       | Agreed start date of insurance contract                                                                                                                            | ISO date format:  YYYY-MM-DD                                    |
| 2     | contractEndDate            | DATE      | O          | No       | Agreed end date of insurance contract                                                                                                                              | ISO date format:  YYYY-MM-DD                                    |
| 2     | insuredAmount              | AMOUNT    | M          | No       | Insured amount value in Property insurance contract (_embedded AMOUNT type)                                                                                        | Fields value, precision, currency                               |

+ Parameters
    + id (TEXT) ... internal ID of the Insurance contract used as part of URI.

+ Model

    + Body

            {
                "id": "0000000000519638",
                "type": "LIFE",
                "product": "V-PER-L-ERA_12",
                "productI18N": "Endowment and whole life insurance",
                "description": "Life insurance with additional tariff without Indexing",
                "alias": "Life insurance for my family",
                "insurancePolicyHolder": "Walter Test",
                "policyNumber": "41630240",
                "status": "ACTIVE",
                "life": {
                    "premiumPaymentInterval": "YEARLY",
                    "premium": {
                        "value": 53703,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "contractStartDate": "2014-03-01",
                    "contractEndDate": "2034-03-01",
                    "insuredAmount": {
                        "value": 2900000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "currentCapitalValue": {
                        "value": 234055,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "expectedPayoffAmount": {
                       "value": 4284600,
                        "precision": 2,
                        "currency": "EUR"
                    }
                }
            }

### Retrieve single Insurance of user [GET]
Returns the information about one specific Insurance product (identified by ID) based on Insurance contract.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Insurance** resource containing details of one user Insurance contract identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Insurance][]

### Update single Insurance of user [PUT]
Allows to change a limited set of Insurance settings of one specific Insurance contract. Currently only the field *alias* can be changed.

Even though other (not editable) fields are not stored they must fulfill the validation criteria of Insurance resource. *Id* in URL, *id* field in payload: These fields must refer to the same Insurance contract, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

A **Insurance** resource with updated *alias* attribute. 

#### Reply
A **Insurance** resource with updated details of one user Insurance contract identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name          | Type/Enum   | Mand/Opt | Attribute description                                  | Expected values/format                                         |
|-------|-------------------------|-------------|----------|--------------------------------------------------------|----------------------------------------------------------------|
| 1     | insurance               | Insurance   | M        | Insurance object                                        |                                                                |
| 1     | signInfo                | SIGNINFO    | M        | SignInfo Details                                       |                                                                |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | id             | The provided ID does not exist.
`ID_MISMATCH`    | id             | The given ID in payload doesn’t match to the ID in URI.
`FIELD_TOO_LONG` | alias          | Length of the provided alias is greater than 60.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "0000000000519638",
                "type": "LIFE",
                "product": "V-PER-L-ERA_12",
                "productI18N": "Endowment and whole life insurance",
                "description": "Life insurance with additional tariff without Indexing",
                "alias": "MY NEW ALIAS - insurance for my family",
                "insurancePolicyHolder": "Walter Test",
                "policyNumber": "41630240",
                "status": "ACTIVE",
                "life": {
                    "premiumPaymentInterval": "YEARLY",
                    "premium": {
                        "value": 53703,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "contractStartDate": "2014-03-01",
                    "contractEndDate": "2034-03-01",
                    "insuredAmount": {
                        "value": 2900000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "currentCapitalValue": {
                        "value": 234055,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "expectedPayoffAmount": {
                       "value": 4284600,
                        "precision": 2,
                        "currency": "EUR"
                    }
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "insurance": {
                    "id": "0000000000519638",
                    "type": "LIFE",
                    "product": "V-PER-L-ERA_12",
                    "productI18N": "Endowment and whole life insurance",
                    "description": "Life insurance with additional tariff without Indexing",
                    "alias": "MY NEW ALIAS - insurance for my family",
                    "insurancePolicyHolder": "Walter Test",
                    "policyNumber": "41630240",
                    "status": "ACTIVE",
                    "life": {
                        "premiumPaymentInterval": "YEARLY",
                        "premium": {
                            "value": 53703,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "contractStartDate": "2014-03-01",
                        "contractEndDate": "2034-03-01",
                        "insuredAmount": {
                            "value": 2900000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "currentCapitalValue": {
                            "value": 234055,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "expectedPayoffAmount": {
                            "value": 4284600,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    }
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016001334"
                }
            }


## InsuranceList [/netbanking/my/contracts/insurances{?size,page}]
Resource Insurance List represents collection of Insurance contracts to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **Insurance** resource items.

Description of **InsuranceList** resource attributes: 

| Level | Attribute name | Type/Enum          | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|--------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER            | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER            | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER            | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER            | M        | Provided or defaulted page size                                      |                          |
| 1     | insurances     | ARRAY of Insurance | O        | Array of Insurance contacts accessible by the user (could be empty) (embedded Insurance resource) |    |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 10,
                "insurances": [
                    {
                        "id": "0000000000392729",
                        "type": "PENSION",
                        "product": "V-PER-L-PEB_P47T",
                        "productI18N": "Immediate annuity pension plan",
                        "description": "Pension insurance without Indexing",
                        "insurancePolicyHolder": "Walter Test",
                        "policyNumber": "20007047",
                        "status": "ACTIVE",
                        "pension": {
                            "premiumPaymentInterval": "ONCE",
                            "premium": {
                                "value": 296948,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "retirementDate": "2014-10-01",
                            "guaranteedPensionCapital": {
                                "value": 638008,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "payoffMethodPension": "YEARLY",
                            "guaranteedPension": {
                                "value": 38408,
                                "precision": 2,
                                "currency": "EUR"
                            }
                        }
                    },
                    {
                        "id": "0000000000519638",
                        "type": "LIFE",
                        "product": "V-PER-L-ERA_12",
                        "productI18N": "Endowment and whole life insurance",
                        "description": "Life insurance with additional tariff without Indexing",
                        "alias": "MY NEW ALIAS - insurance for my family",
                        "insurancePolicyHolder": "Walter Test",
                        "policyNumber": "41630240",
                        "status": "ACTIVE",
                        "life": {
                            "premiumPaymentInterval": "YEARLY",
                            "premium": {
                                "value": 53703,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "contractStartDate": "2014-03-01",
                            "contractEndDate": "2034-03-01",
                            "insuredAmount": {
                                "value": 2900000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "currentCapitalValue": {
                                "value": 234055,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "expectedPayoffAmount": {
                                "value": 4284600,
                                "precision": 2,
                                "currency": "EUR"
                            }
                        }
                    }
                ]
            }

### Get a list of Insurance contracts for current user [GET]
Get possibly empty list of all Insurance contracts this user owns. This call is paginated and sorted always by ID (newer contract first).

**Note:** Closed Insurance contracts could be displayed too, because BackEnd can provide this information 90 days (business parameter) after contract closing.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **InsuranceList** with possibly empty (omitted) array of *embedded* **Insurance** items without transaction data.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [InsuranceList][]


# Group Buildings
BuildingSaving-Loan related resources of *Banking Services API*.

## BuildingSaving-Loan [/netbanking/my/contracts/buildings/{id}]
BuildingSaving-Loan resource represents user building product of different types (building saving, building loan).

Description of **BuildingSaving-Loan** resource attributes: 

| Level | Attribute name             | Type/Enum | Mand/Opt   | Editable | Attribute description                                                                                                                                              | Expected values/format                                      |
|-------|----------------------------|-----------|------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| 1     | id                         | ID        | M          | No       | Internal ID as reference for building account                                                                                                                      |                                                             |
| 1     | accountno                  | ACCOUNTNO | O          | No       | Building Account number (_embedded ACCOUNTNO type)                                                                                                                 |                                                             |
| 1     | type                       | ENUM      | M          | No       | Product Type of building product (Saving, Loan)                                                                                                                    | ENUM values: [BUILD_SAVING, BUILD_LOAN]                     |
| 1     | product                    | TEXT      | M          | No       | Building product name. Localization of expected values and George FE behavior is needed, because Erste Group doesn't use harmonized group product definitions.     | Values in AT: `bausparvertrag`, `bauspardarlehen`           |
| 1     | productI18N                | TEXT      | M          | No       | Localized building product name depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language).      | Values e.g.: `Building Savings`, `Erste Building Loan`      |
| 1     | description                | TEXT      | O          | No       | Description - Building Account name, Name of principal account holder                                                                                              |                                                             |
| 1     | alias                      | TEXT60    | O          | Yes      | Building Account/contract alias stored in BackEnd (could be reused in mobile app as well).                                                                         |                                                             |
| 1     | balance                    | AMOUNT    | M          | No       | Saved amount for Building Saving, Outstanding debt amount for Building Loan. Balance is provided only if account is not offline/in closing (_embedded AMOUNT type) | Fields value, precision, currency                           |
| 1     | status                     | ENUM      | O          | No       | The status of the specific building product                                                                                                                        | ENUM values: [ACTIVE, CLOSED]                               |
| 1     | contractHolders            | ARRAY of TEXT | O      | No       | Contract holders - list of their full names                                                                                                                        | format "FirstName MiddleName LastName"                      |
| 1     | creditInterestRate         | FLOAT     | O          | No       | Basic credit Interest rate, used for Building Saving deposits                                                                                                      | Value in percentage, e.g. 1,5 will be displayed as 1,5 %     |
| 1     | debitInterestRate          | FLOAT     | O          | No       | Basic debit Interest rate, used for Building Loan                                                                                                                  | Value in percentage, e.g. 9,5 will be displayed as 9,5 %     |
| 1     | saving                     | structure | O          | No       | Structure for Building Saving accounts                                                                                                                             |                                                             |
| 2     | targetAmount               | AMOUNT    | O          | No       | Target amount of Building Saving account (_embedded AMOUNT type)                                                                                                   | Fields value, precision, currency                           |
| 2     | agreedMonthlySavings       | AMOUNT    | O          | No       | Agreed monthly deposits to Building Savings (_embedded AMOUNT type)                                                                                                | Fields value, precision, currency                           |
| 2     | bonusBearingMonthlySavings | AMOUNT    | O          | No       | Max Bonus bearing monthly deposits to Building Savings (_embedded AMOUNT type)                                                                                     | Fields value, precision, currency                           |
| 2     | expiryDate                 | DATETIME  | O          | No       | Expiration date of Building Saving contract (end of period with not allowed withdrawal, e.g. 6 years only savings) or Expiration notice date (e.g. 3 months after announcement) | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ     |
| 2     | bonusBearingDepositDone    | AMOUNT    | O          | No       | Total of Bonus bearing monthly deposits to Building Savings done this year (_embedded AMOUNT type)                                                                 | Fields value, precision, currency                           |
| 2     | bonusBearingDepositToPay   | AMOUNT    | O          | No       | Remaining deposit to be paid to Building Savings till the end of this year to get annual Maximal Bonus (_embedded AMOUNT type)                                     | Fields value, precision, currency                           |
| 1     | loan                       | structure | O          | No       | Structure for Building Loan accounts                                                                                                                               |                                                             |
| 2     | loanAmount                 | AMOUNT    | M          | No       | Total contracted Building Loan amount value (_embedded AMOUNT type)                                                                                                | Fields value, precision, currency                           |
| 2     | loanInstallment            | AMOUNT    | M          | No       | Loan Installment - monthly repayment amount for Building Saving Loan, Bridge Loan (_embedded AMOUNT type)                                                          | Fields value, precision, currency                           |
| 2     | additionalSavings          | AMOUNT    | O          | No       | Optional additional savings for Bridge Loan (_embedded AMOUNT type)                                                                                                | Fields value, precision, currency                           |
| 2     | paymentInsurance           | AMOUNT    | O          | No       | Optional payment protection insurance - monthly fee (_embedded AMOUNT type)                                                                                        | Fields value, precision, currency                           |
| 2     | nextInstallmentDate        | DATE      | O          | No       | Next Installment date                                                                                                                                              | ISO date format                                             |
| 2     | interestRateFromDate       | DATE      | O          | No       | Current interest rate is valid/fixed from this date. This date can be provided only when interest rate is variable (not fixed).                                    | ISO date format                                             |
| 2     | interestRateToDate         | DATE      | O          | No       | Current interest rate is valid/fixed to this date. This date is provided only when interest rate is variable (not fixed).                                          | ISO date format                                             |
| 1     | flags                      | FLAGS     | O          | No       | Array of optional Flag values, the absence of a certain string is considered as “false”                                                                            | Flags values: `notRequestedBonus` - (state) bonus for Building Saving was not requested by client  |

+ Parameters
    + id (TEXT) ... internal ID of the BuildingSaving-Loan account used as part of URI.

+ Model

    + Body

            {
                "id": "3200219330",
                "type": "BUILD_SAVING",
                "product": "bausparvertrag",
                "productI18N": "Building Savings",
                "description": "Helmut Fuchs account name",
                "alias": "Helmut Fuchs alias",
                "balance": {
                    "value": 80634,
                    "precision": 2,
                    "currency": "EUR"
                },
                "status": "ACTIVE",
                "contractHolders": [
                    "Walter Test",
                    "Helmut Fuchs"
                ],
                "creditInterestRate": 2.3,
                "saving": {
                    "targetAmount": {
                        "value": 2480000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "agreedMonthlySavings": {
                        "value": 17500,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "bonusBearingMonthlySavings": {
                        "value": 20000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "expiryDate": "2014-02-14T23:00:00Z",
                    "bonusBearingDepositDone": {
                        "value": 80000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "bonusBearingDepositToPay": {
                        "value": 160000,
                        "precision": 2,
                        "currency": "EUR"
                    }
                },
                "flags": [
                ]
            }

### Retrieve single BuildingSaving-Loan of user [GET]
Returns the information about one specific Building product account (identified by ID) based on Building Society contract.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **BuildingSaving-Loan** resource containing details of one user Building Society contract identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | id       | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [BuildingSaving-Loan][]

### Update single BuildingSaving-Loan of user [PUT]
Allows to change a limited set of Building Society contract settings of one specific Building account. Currently only the field *alias* can be changed.

Even though other (not editable) fields are not stored they must fulfill the validation criteria of BuildingSaving-Loan resource. *Id* in URL, *id* field in payload: These fields must refer to the same Building account, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

A **BuildingSaving-Loan** resource with updated *alias* attribute. 

#### Reply
A **BuildingSaving-Loan** resource with updated details of one user Building account identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name       | Type/Enum               | Mand/Opt | Attribute description                 | Expected values/format                                   |
|-------|----------------------|-------------------------|----------|---------------------------------------|----------------------------------------------------------|
| 1     | building             | BuildingSaving-Loan     | M        | BuildingSaving-Loan object            |                                                          |
| 1     | signInfo             | SIGNINFO                | M        | SignInfo Details                      |                                                          |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | building       | The provided ID does not exist.
`ID_MISMATCH`    | id             | The given ID in payload doesn’t match to the ID in URI.
`FIELD_TOO_LONG` | alias          | Length of the provided alias is greater than 60.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "3200219330",
                "type": "BUILD_SAVING",
                "product": "bausparvertrag",
                "productI18N": "Building Savings",
                "description": "Helmut Fuchs account name",
                "alias": "NEW Helmut Fuchs alias",
                "balance": {
                    "value": 80634,
                    "precision": 2,
                    "currency": "EUR"
                },
                "status": "ACTIVE",
                "contractHolders": [
                    "Walter Test",
                    "Helmut Fuchs"
                ],
                "creditInterestRate": 2.3,
                "saving": {
                    "targetAmount": {
                        "value": 2480000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "agreedMonthlySavings": {
                        "value": 17500,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "bonusBearingMonthlySavings": {
                        "value": 20000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "expiryDate": "2014-02-14T23:00:00Z",
                    "bonusBearingDepositDone": {
                        "value": 80000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "bonusBearingDepositToPay": {
                        "value": 160000,
                        "precision": 2,
                        "currency": "EUR"
                    }
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "building": {
                    "id": "3200219330",
                    "type": "BUILD_SAVING",
                    "product": "bausparvertrag",
                    "productI18N": "Building Savings",
                    "description": "Helmut Fuchs account name",
                    "alias": "NEW Helmut Fuchs alias",
                    "balance": {
                        "value": 80634,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "status": "ACTIVE",
                    "contractHolders": [
                        "Walter Test",
                        "Helmut Fuchs"
                    ],
                    "creditInterestRate": 2.3,
                    "saving": {
                        "targetAmount": {
                            "value": 2480000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "agreedMonthlySavings": {
                            "value": 17500,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bonusBearingMonthlySavings": {
                            "value": 20000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "expiryDate": "2014-02-14T23:00:00Z",
                        "bonusBearingDepositDone": {
                            "value": 80000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "bonusBearingDepositToPay": {
                            "value": 160000,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    }
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000016001334"
                }
            }


## BuildingSaving-LoanList [/netbanking/my/contracts/buildings{?size,page}]
Resource BuildingSaving-Loan List represents collection of Building Society contracts to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **BuildingSaving-Loan** resource items.

Description of **BuildingSaving-LoanList** resource attributes: 

| Level | Attribute name | Type/Enum         | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|-------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER           | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER           | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER           | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER           | M        | Provided or defaulted page size                                      |                          |
| 1     | buildings      | ARRAY of BuildingSaving-Loan | O        | Array of BUilding contacts accessible by the user (could be empty) (embedded BuildingSaving-Loan resource) |    |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 2,
                "nextPage": 1,
                "pageSize": 2,
                "buildings": [
                    {
                        "id": "3200219330",
                        "type": "BUILD_SAVING",
                        "product": "bausparvertrag",
                        "productI18N": "Building Savings",
                        "description": "Helmut Fuchs account name",
                        "alias": "Helmut Fuchs alias",
                        "balance": {
                            "value": 80634,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "status": "ACTIVE",
                        "contractHolders": [
                            "Walter Test",
                            "Helmut Fuchs"
                        ],
                        "creditInterestRate": 2.3,
                        "saving": {
                            "targetAmount": {
                                "value": 2480000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "agreedMonthlySavings": {
                                "value": 17500,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "bonusBearingMonthlySavings": {
                                "value": 20000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "expiryDate": "2014-02-14T23:00:00Z",
                            "bonusBearingDepositDone": {
                                "value": 80000,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "bonusBearingDepositToPay": {
                                "value": 160000,
                                "precision": 2,
                                "currency": "EUR"
                            }
                        }
                    },
                    {
                        "id": "4917048624",
                        "type": "BUILD_LOAN",
                        "product": "bauspardarlehen",
                        "productI18N": "Building Loan",
                        "description": "Anton Rothschopf account name",
                        "alias": "Anton Rothschopf alias",
                        "balance": {
                            "value": -812334,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "status": "ACTIVE",
                        "contractHolders": [
                            "Anton Rothschopf"
                        ],
                        "debitInterestRate": 12.3,
                        "loan" {
                            "loanAmount": {
                                "value": 4069668,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "loanInstallment": {
                                "value": 40436,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "paymentInsurance" : {
                                "value": 530,
                                "precision": 2,
                                "currency": "EUR"
                            },
                            "interestRateToDate": "2015-02-01T23:00:00Z"
                        }
                    }
                ]
            }

### Get a list of Building contracts for current user [GET]
Get possibly empty list of all Building Saving and Loan contracts this user owns. This call is paginated and sorted always by ID (newer contract first).

**Note:** Closed Building contracts could be displayed too, because BackEnd can provide this information 90 days (business parameter) after contract closing.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **BuildingSaving-LoanList** with possibly empty (omitted) array of *embedded* **BuildingSaving-Loan** items without transaction data.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [BuildingSaving-LoanList][]


## BuildingTransactionList [/netbanking/my/contracts/buildings/{id}/transactions{?size,page}]
Resource Building Transaction List represents collection of BuildingSaving-Loan transactions booked on account identified by ID.
This resource consists of paging attributes and array of transaction structures.

Description of **BuildingTransactionList** resource attributes: 

| Level | Attribute name | Type/Enum | Mand/Opt | Attribute description                                                          | Expected values/format                       |
|-------|----------------|-----------|----------|--------------------------------------------------------------------------------|----------------------------------------------|
| 1     | pageNumber     | INTEGER   | M        | Page number of returned page, starting from 0 for the first page               |                                              |
| 1     | pageCount      | INTEGER   | M        | Total number of pages of defined size                                          |                                              |
| 1     | nextPage       | INTEGER   | O        | Page number of following page (provided only when exist)                       |                                              |
| 1     | pageSize       | INTEGER   | M        | Provided or defaulted page size                                                |                                              |
| 1     | transactions   | ARRAY of  | O        | Array of Building transactions (could be empty)                                |                                              |
| 2     | amount         | AMOUNT    | M        | Building transaction amount (_embedded AMOUNT type)                            | Fields value, precision, currency            |
| 2     | valuationDate  | DATE      | M        | Building transaction valuation date                                            | ISO date format:  YYYY-MM-DD                 |
| 2     | bookingText    | TEXT      | M        | Building transaction booking text, description                                 |                                              |

+ Parameters
    + id (TEXT, required) ... internal ID of the Building Society contract used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 100,
                "transactions": [
                    {
                        "amount": {
                            "value": 58200,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "valuationDate": "2014-02-20",
                        "bookingText": "Building Saving deposit"
                    },
                    {
                        "amount": {
                            "value": -250,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "valuationDate": "2014-01-31",
                        "bookingText": "Account monthly fee"
                    },
                    {
                        "amount": {
                            "value": 20200,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "valuationDate": "2014-01-20",
                        "bookingText": "Building Saving deposit"
                    }
                ]
            }    

### Get a list of transactions for one contract [GET]
Get possibly empty list of transactions of current and previous year for given Building Society contract. This call is paginated and always sorted by *valuationDate*, no other sorts are supported due to performance reasons. 

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **BuildingTransactionList** with possibly empty (omitted) array of building transaction items.

#### Error codes
Error code     | Scope          | Purpose
---------------|----------------|------------------------------------
`ID_NOT_FOUND` | building       | The provided contract ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
            

+ Response 200 (application/json)

    [BuildingTransactionList][]


# Group Leasings
Leasing related resources of *Banking Services API*.

## Leasing [/netbanking/my/contracts/leasings/{id}]
Leasing resource represents user Leasing contract product of different types (Erste bank financial leasing, movable assets leasing, car leasing, hire&purchase leasing).

Description of **Leasing** resource attributes: 

| Level | Attribute name             | Type/Enum | Mand/Opt   | Editable | Attribute description                                                                                                                                              | Expected values/format                                                              |
|-------|----------------------------|-----------|------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| 1     | id                         | ID        | M          | No       | Internal ID as reference for leasing contract                                                                                                                      |                                                                                     |
| 1     | type                       | ENUM      | M          | No       | Product Type of leasing (EB financial/capital lease, movable asset lease, car lease, hire&purchase lease)                                                          | ENUM values: [EBV_LEASING, MOVABLE_ASSET_LEASING, CAR_LEASING, HIRE_PURCHASE]       |
| 1     | product                    | TEXT      | M          | No       | Product name of leasing. Localization of expected values and George FE behavior is needed, because Erste Group doesn't use harmonized group product definitions.   | Values in AT, e.g. : `KFZ-Leasing`, `Mietkauf`                                      |
| 1     | productI18N                | TEXT      | M          | No       | Localized product name of leasing depending on Accept-Language header field (if user preferred language is stored in BE, then localized name in this language).    | Values in AT, e.g. : `Company Car leasing`, `First Amendment Purchase Lease`        |
| 1     | description                | TEXT      | O          | No       | Customized description about the contract (object of the leasing)                                                                                                  |                                                                                     |
| 1     | alias                      | TEXT60    | O          | Yes      | Leasing contract alias stored in BackEnd (could be reused in mobile app as well)                                                                                   |                                                                                     |
| 1     | contractHolders            | ARRAY of TEXT | O      | No       | Contract holders - list of their full names                                                                                                                        | format "FirstName MiddleName LastName"                                              |
| 1     | purchasePrice              | AMOUNT    | M          | No       | Price of leasing object, car price including VAT (_embedded AMOUNT type)                                                                                           | Fields value, precision, currency                                                   |
| 1     | purchasePriceDownPayment   | AMOUNT    | O          | No       | Down/advance payment for the leasing object before lease (_embedded AMOUNT type)                                                                                   | Fields value, precision, currency                                                   |
| 1     | stipulationOfRent          | AMOUNT    | O          | No       | One-time payment paid at the beginning of the lease, which reduces the total cost of investment and leasing installment is reduced too. (_embedded AMOUNT type)    | Fields value, precision, currency                                                   |
| 1     | deposit                    | AMOUNT    | O          | No       | One-time payment made at the beginning of the lease, which will be repayed at the expiration of the contract or reducing purchase price. (_embedded AMOUNT type)   | Fields value, precision, currency                                                   |
| 1     | promotion                  | AMOUNT    | O          | No       | Payment of promotion (bank guarantee); the total cost of investment is reduced by the promotion and leasing installment is reduced too. (_embedded AMOUNT type)    | Fields value, precision, currency                                                   |
| 1     | duration                   | INTEGER   | M          | No       | Duration of contract in months                                                                                                                                     |                                                                                     |
| 1     | startOfContract            | DATETIME  | M          | No       | Start date of Leasing contract                                                                                                                                     | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                          |
| 1     | endOfContract              | DATETIME  | M          | No       | End date of Leasing contract                                                                                                                                       | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                          |
| 1     | residualValue              | AMOUNT    | M          | No       | Remaining value of leasing object after expiration of contract (possible re-purchase price). (_embedded AMOUNT type)                                               | Fields value, precision, currency                                                   |
| 1     | finalInstallment           | AMOUNT    | O          | No       | The last installment payment at the end of contract. (_embedded AMOUNT type)                                                                                       | Fields value, precision, currency                                                   |
| 1     | remainingInstallments      | AMOUNT    | O          | No       | Sum of currently outstanding remaining installment payments till the end of contract. (_embedded AMOUNT type)                                                      | Fields value, precision, currency                                                   |
| 1     | currentInstallment         | AMOUNT    | M          | No       | Current installment payment to be paid next due date. (_embedded AMOUNT type)                                                                                      | Fields value, precision, currency                                                   |
| 1     | inArrearsWithPayment       | AMOUNT    | O          | No       | Amount of outstanding payments after due date. (_embedded AMOUNT type)                                                                                             | Fields value, precision, currency                                                   |
| 1     | remainingTerm              | INTEGER   | M          | No       | Number of months until the end of contract is reached                                                                                                              |                                                                                     |
| 1     | nextDueDate                | DATETIME  | M          | No       | Due Date of the next installment to be paid                                                                                                                        | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                                          |
| 1     | currentMethodOfPayment     | ENUM      | M          | No       | Method of installment payment                                                                                                                                      | ENUM values: [TRANSFER, COLLECTION]                                                 |
| 1     | currentMethodOfPaymentI18N | TEXT      | O          | No       | Localized Method of payment (internationalized)                                                                                                                    | Values in AT, e.g. : `Zahlschein`, `Direct debit collection`                        |
| 1     | bankName                   | TEXT      | M          | No       | Bank name of leasing offerer                                                                                                                                       |                                                                                     |
| 1     | accountno                  | ACCOUNTNO | O          | No       | Account information of leasing offerer (_embedded ACCOUNTNO type)                                                                                                  |                                                                                     |

+ Parameters
    + id (TEXT) ... internal ID of the Leasing contract used as part of URI.

+ Model

    + Body

            {
                "id": "0899-06-K000007",
                "type": "CAR_LEASING",
                "product": "KFZ-Leasing",
                "productI18N": "Company car leasing",
                "description": "VW (D) Multivan Highline 3,2 V6 4motion",
                "alias": "Helmut Fuchs alias",
                "purchasePrice": {
                    "value": 5164083,
                    "precision": 2,
                    "currency": "EUR"
                },
                "stipulationOfRent": {
                    "value": 1032817,
                    "precision": 2,
                    "currency": "EUR"
                },
                "deposit": {
                    "value": 500000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "promotion": {
                    "value": 0,
                    "precision": 2,
                    "currency": "EUR"
                },
                "duration": 60,
                "startOfContract": "2006-10-31T23:00:00Z",
                "endOfContract": "2011-09-29T22:00:00Z",
                "residualValue": {
                    "value": 852074,
                    "precision": 2,
                    "currency": "EUR"
                },
                "currentInstallment": {
                    "value": 51590,
                    "precision": 2,
                    "currency": "EUR"
                },
                "inArrearsWithPayment": {
                    "value": 0,
                    "precision": 2,
                    "currency": "EUR"
                },
                "remainingTerm": 0,
                "nextDueDate": "2011-09-29T22:00:00Z",
                "currentMethodOfPayment": "TRANSFER",
                "currentMethodOfPaymentI18N": "Zahlschein",
                "bankName": "Erste Bank",
                "accountNo": {
                    "number": 23421,
                    "bankCode": 20111
                }                
            }

### Retrieve single Leasing contract of user [GET]
Returns the information about one specific Leasing product (identified by ID) based on Leasing contract.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **Leasing** resource containing details of one user Leasing contract identified by parameter ID.

#### Error codes
Error code     | Scope    | Purpose
---------------|----------|------------------------------------
`ID_NOT_FOUND` | leasing  | The provided ID does not exist.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Leasing][]

### Update single Leasing of user [PUT]
Allows to change a limited set of Leasing settings of one specific Leasing contract identified by ID. Currently only the field *alias* can be changed.

Even though other (not editable) fields are not stored they must fulfill the validation criteria of Leasing resource. *Id* in URL, *id* field in payload: These fields must refer to the same Leasing, else an error is returned.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

A **Leasing** resource with updated *alias* attribute. 

#### Reply
A **Leasing** resource with updated details of one user Leasing contract identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name       | Type/Enum               | Mand/Opt | Attribute description                 | Expected values/format                                   |
|-------|----------------------|-------------------------|----------|---------------------------------------|----------------------------------------------------------|
| 1     | leasing              | Leasing                 | M        | Leasing object                        |                                                          |
| 1     | signInfo             | SIGNINFO                | M        | SignInfo Details                      |                                                          |

#### Error codes
Error code       | Scope          | Purpose
-----------------|----------------|------------------------------------
`ID_NOT_FOUND`   | leasing        | The provided ID does not exist.
`ID_MISMATCH`    | id             | The given ID in payload doesn’t match to the ID in URI.
`FIELD_TOO_LONG` | alias          | Length of the provided alias is greater than 60.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "0899-06-K000007",
                "type": "CAR_LEASING",
                "product": "KFZ-Leasing",
                "productI18N": "Company car leasing",
                "description": "VW (D) Multivan Highline 3,2 V6 4motion",
                "alias": "UPDATED alias",
                "purchasePrice": {
                    "value": 5164083,
                    "precision": 2,
                    "currency": "EUR"
                },
                "stipulationOfRent": {
                    "value": 1032817,
                    "precision": 2,
                    "currency": "EUR"
                },
                "deposit": {
                    "value": 500000,
                    "precision": 2,
                    "currency": "EUR"
                },
                "promotion": {
                    "value": 0,
                    "precision": 2,
                    "currency": "EUR"
                },
                "duration": 60,
                "startOfContract": "2006-10-31T23:00:00Z",
                "endOfContract": "2011-09-29T22:00:00Z",
                "residualValue": {
                    "value": 852074,
                    "precision": 2,
                    "currency": "EUR"
                },
                "currentInstallment": {
                    "value": 51590,
                    "precision": 2,
                    "currency": "EUR"
                },
                "inArrearsWithPayment": {
                    "value": 0,
                    "precision": 2,
                    "currency": "EUR"
                },
                "remainingTerm": 0,
                "nextDueDate": "2011-09-29T22:00:00Z",
                "currentMethodOfPayment": "TRANSFER",
                "currentMethodOfPaymentI18N": "Zahlschein",
                "bankName": "Erste Bank",
                "accountNo": {
                    "number": 23421,
                    "bankCode": 20111
                }                
            }

+ Response 200 (application/json)

    + Body

            {
                "leasing": {
                    "id": "0899-06-K000007",
                    "type": "CAR_LEASING",
                    "product": "KFZ-Leasing",
                    "productI18N": "Company car leasing",
                    "description": "VW (D) Multivan Highline 3,2 V6 4motion",
                    "alias": "UPDATED alias",
                    "purchasePrice": {
                        "value": 5164083,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "stipulationOfRent": {
                        "value": 1032817,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "deposit": {
                        "value": 500000,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "promotion": {
                        "value": 0,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "duration": 60,
                    "startOfContract": "2006-10-31T23:00:00Z",
                    "endOfContract": "2011-09-29T22:00:00Z",
                    "residualValue": {
                        "value": 852074,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "currentInstallment": {
                        "value": 51590,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "inArrearsWithPayment": {
                        "value": 0,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "remainingTerm": 0,
                    "nextDueDate": "2011-09-29T22:00:00Z",
                    "currentMethodOfPayment": "TRANSFER",
                    "currentMethodOfPaymentI18N": "Zahlschein",
                    "bankName": "Erste Bank",
                    "accountNo": {
                        "number": 23421,
                        "bankCode": 20111
                    }                
                },
                "signInfo": {
                    "state": "OPEN",
                    "signId": "043971701790000017701334"
                }
            }


## LeasingList [/netbanking/my/contracts/leasings{?size,page,sort,order}]
Resource Leasing List represents collection of Leasing contracts to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **Leasing** resource items.

Description of **LeasingList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | leasings       | ARRAY of Leasing | O        | Array of Leasing contacts accessible by the user (could be empty) (embedded Leasing resource) |    |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + sort (TEXT, optional) ... Possible sort fields are: `id` and `startOfContract`. If no sort is given, a random order has to be assumed that can change between calls.
    + order (TEXT, optional) ... Sorting order can be either `asc` or `desc` (case insensitive), with `asc` as default. Sorting multiple fields at the same time is possible by comma-separating the sorting fields and their corresponding sort orders. Sort priorities are left to right, so within the first field it is sorted by the second etc. Missing corresponding order entries are considered to be asc.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 10,
                "leasings": [
                    {
                        "id": "0899-06-K000007",
                        "type": "CAR_LEASING",
                        "product": "KFZ-Leasing",
                        "productI18N": "Company car leasing",
                        "description": "VW (D) Multivan Highline 3,2 V6 4motion",
                        "alias": "UPDATED alias",
                        "purchasePrice": {
                            "value": 5164083,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "stipulationOfRent": {
                            "value": 1032817,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "deposit": {
                            "value": 500000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "promotion": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "duration": 60,
                        "startOfContract": "2009-10-31T23:00:00Z",
                        "endOfContract": "2014-10-29T22:00:00Z",
                        "residualValue": {
                            "value": 852074,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "currentInstallment": {
                            "value": 51590,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "inArrearsWithPayment": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "remainingTerm": 0,
                        "nextDueDate": "2014-10-29T22:00:00Z",
                        "currentMethodOfPayment": "TRANSFER",
                        "currentMethodOfPaymentI18N": "Zahlschein",
                        "bankName": "Erste Bank",
                        "accountNo": {
                            "number": 23421,
                            "bankCode": 20111
                        }                
                    },
                    {
                        "id": "0040-02-M000001",
                        "type": "HIRE_PURCHASE",
                        "product": "Mietkauf",
                        "productI18N": "First Amendment Purchase Lease",
                        "description": "Eurofighter Typhoon twin-engine, canard-delta wing, multirole fighter",
                        "alias": "Eurofighter super lease",
                        "purchasePrice": {
                            "value": 200000000000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "purchasePriceDownPayment": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "stipulationOfRent": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "deposit": {
                            "value": 500000000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "promotion": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "duration": 120,
                        "startOfContract": "2004-12-31T23:00:00Z",
                        "endOfContract": "2014-12-31T23:00:00Z",
                        "residualValue": {
                            "value": 200000099,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "finalInstallment": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "remainingInstallments": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "currentInstallment": {
                            "value": 2058323697,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "inArrearsWithPayment": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "remainingTerm": 0,
                        "nextDueDate": "2014-12-31T22:00:00Z",
                        "currentMethodOfPayment": "TRANSFER",
                        "currentMethodOfPaymentI18N": "Zahlschein",
                        "bankName": "Erste Bank",
                        "accountNo": {
                            "number": 23421,
                            "bankCode": 20111
                        }                
                    }
                ]
            }

### Get a list of Leasing contracts for current user [GET]
Get possibly empty list of all Leasing contracts this user owns. This call is paginated and can be sorted.

**Note:** Closed Leasing contracts could be displayed too, because BackEnd can provide this information 90 days (business parameter) after contract closing.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **LeasingList** with possibly empty (omitted) array of *embedded* **Leasing** items without transaction data.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [LeasingList][]


# Group Securities
Investment Securities related resources of *Banking Services API*.

## SecuritiesAccount [/netbanking/my/securities/{id}]
Securities Account resource represents special portfolio account which is used to administrate securities of a user. The securities are organized in Securities SubAccounts, which contains collection of Securities Titles. Securities Account can contain several Securities Sub Accounts, which can contain several Securities Titles. One Securities Title represents one investment position.

Description of **SecuritiesAccount** resource attributes: 

| Level | Attribute name      | Type/Enum | Mand/Opt   | Editable | Attribute description                                                                                    | Expected values/format                                      |
|-------|---------------------|-----------|------------|----------|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| 1     | id                  | ID        | M          | No       | Internal ID as reference for Securities portfolio account                                                |                                                             |
| 1     | accountno           | TEXT      | M          | No       | Securities portfolio Account identification number                                                       | IBAN in AT, MUIN in CZ, CTS ownerId in SK                   |
| 1     | description         | TEXT      | M          | No       | Description - Securities portfolio Account name, Name of principal account holder                        |                                                             |
| 1     | alias               | TEXT60    | O          | Yes      | Securities portfolio account alias stored in BackEnd (could be reused in mobile app as well).            |                                                             |
| 1     | balance             | AMOUNT    | M          | No       | Current balance amount of Securities portfolio account. (_embedded AMOUNT type)                          | Fields value, precision, currency                           |
| 1     | settlementAccount   | ACCOUNTNO | O          | No       | Main clearing account used for investment settlement (_embedded ACCOUNTNO type)                          |                                                             |
| 1     | subSecAccounts      | ARRAY of  | O          | No       | Collection of Securities Sub accounts belonging to Securities Account                                    |                                                             |
| 2     | id                  | ID        | M          | No       | Internal ID as reference for Securities Sub account                                                      |                                                             |
| 2     | name                | TEXT      | O          | No       | Name of Securities Sub account.                                                                          |                                                             |
| 2     | titles              | ARRAY of  | O          | No       | Collection of Securities Titles belonging to particular Sub account                                      |                                                             |
| 3     | title               | TEXT      | M          | No       | Title name of securities investment                                                                      |                                                             |  
| 3     | isin                | TEXT      | M          | No       | International Securities Identification Number (ISIN) uniquely identifies security.                      | Its structure is defined in ISO 6166                        |
| 3     | numberOfShares      | FLOAT     | M          | No       | Number of securities/shares (_embedded AMOUNT type)                                                      |                                                             |
| 3     | lastPrice           | AMOUNT    | M          | No       | Last Price of Securities title (_embedded AMOUNT type)                                                   | Fields value, precision, currency                           |
| 3     | lastPriceDate       | DATETIME  | M          | No       | Date of securities last price evaluation                                                                 | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ                  |
| 3     | performance         | AMOUNT    | O          | No       | Performance of Securities title since the purchase (_embedded AMOUNT type)                               | Fields value, precision, currency                           |
| 3     | performancePercent  | FLOAT     | O          | No       | Performance of Securities title in percentage                                                            | Value in percentage, e.g. 1,5 will be displayed as 1,5 %     |
| 3     | marketValue         | AMOUNT    | M          | No       | Market Value of Securities title (_embedded AMOUNT type)                                                 | Fields value, precision, currency                           |
| 3     | totalIncome         | AMOUNT    | O          | No       | Total Income of Securities title (_embedded AMOUNT type) *In what period? From begining of investing to this title?*  | Fields value, precision, currency              |
| 3     | avgPurchasePrice    | AMOUNT    | O          | No       | Average purchase price of Securities title (_embedded AMOUNT type)                                       | Fields value, precision, currency                           |
| 3     | avgPurchaseValue    | AMOUNT    | O          | No       | Average purchase value of Securities title (_embedded AMOUNT type)                                       | Fields value, precision, currency                           |
| 3     | stockExchange       | TEXT      | O          | No       | Name of Stock Exchange where title is listed                                                             |                                                             |
| 3     | securityIndication  | TEXT      | M          | No       | Localized security indication depending on security type and product group                               | Possible values (DE/EN): `Fondsanbieter`/`Fund provider` (if securityType=`FUND`), *Trading provider name* (if securityType=`SHARE` OR securityType=`CERTIFICATE` AND productGroup=`INVESTMENT`,`KNOCK_OUT`), else `Richtkurs`/`Reference quote`  |
| 3     | securityType        | ENUM      | M          | No       | Security Product Type (Could be localized based on country securities offering)                          | ENUM values: [BOND, SHARE, FUND, IPO, OPTION, OTHER, INDEX, CERTIFICATE, INVESTMENT, KNOCKOUT, UNKNOWN]                                             |
| 3     | productGroup        | ENUM      | M          | No       | Security Product Group (Could be localized based on country securities offering)                         | ENUM values: [BONDS_AND_MORE, GUARANTEE_OF_PRINCIPAL, NO_GUARANTEE_OF_PRINCIPAL, REAL_ESTATE, SHARES, STOCK_AND_MIXED, INVESTMENT, KNOCK_OUT, UNKNOWN]   |
| 3     | flags               | FLAGS     | O          | No       | Array of optional Flag values, the absence of a certain string is considered as “false”                  | Flags values: `capitalGainTax`                              |

+ Parameters
    + id (TEXT) ... internal ID of the Securities portfolio account used as part of URI.

+ Model

    + Body

            {
                "id": "2f03203727596531303d17",
                "accountno": "AT642011120031692500",
                "description": "MAXIMILIAN MUSTERMANN1 TEST- ABT. SEC",
                "alias": "Maximilian Mustermann TEST- Securities",
                "balance": {
                    "value": 729079,
                    "precision": 2,
                    "currency": "EUR"
                },
                "settlementAccount": {
                    "number": "20031693700",
                    "bankCode": "20111"
                },
                "subSecAccounts": [
                    {
                        "id": "001",
                        "name": " Security sub account 001",
                        "titles": [
                            {
                                "title": "RORENTO              EO 3",
                                "isin": "ANN757371433",
                                "numberOfShares": 5.40,
                                "lastPrice": {
                                    "value": 538500,
                                    "precision": 4,
                                    "currency": "EUR"
                                },
                                "lastPriceDate": "2014-06-19T22:00:00Z",
                                "performance": {
                                    "value": 1837,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "performancePercent": 6.74,
                                "marketValue": {
                                    "value": 29079,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "totalIncome": {
                                    "value": 0,
                                    "precision": 3,
                                    "currency": "EUR"
                                },
                                "avgPurchasePrice": {
                                    "value": 494400,
                                    "precision": 4,
                                    "currency": "EUR"
                                },
                                "avgPurchaseValue": {
                                    "value": 1457400,
                                    "precision": 4,
                                    "currency": "EUR"
                                },
                                "stockExchange": "SIX SWISS EX",
                                "securityIndication": "Fondsanbieter",
                                "securityType": "FUND",
                                "productGroup": "BONDS_AND_MORE",
                                "flags": [
                                    "capitalGainTax"
                                ]
                            },
                            {
                                "title": "Erste Group EURO STOXX 50 Express-Bond 2019",
                                "isin": "AT0000A188S7",
                                "numberOfShares": 7.00,
                                "lastPrice": {
                                    "value": 100000,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "lastPriceDate": "2014-06-24T22:00:00Z",
                                "performance": {
                                    "value": 7037,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "performancePercent": 7.74,
                                "marketValue": {
                                    "value": 700000,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "totalIncome": {
                                    "value": 0,
                                    "precision": 3,
                                    "currency": "EUR"
                                },
                                "avgPurchasePrice": {
                                    "value": 100000,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "avgPurchaseValue": {
                                    "value": 700000,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "stockExchange": "EURO STOXX 50 Index",
                                "securityIndication": "Reference quote",
                                "securityType": "BOND",
                                "productGroup": "BONDS_AND_MORE",
                                "flags": [
                                    "capitalGainTax"
                                ]
                            }
                        ]
                    }
                ]
            }

### Retrieve single Securities account of user [GET]
Returns the information about one specific Securities portfolio account (identified by ID) with subaccounts and securities titles assigned to this account.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
A **SecuritiesAccount** resource containing details of one user securities portfolio account identified by parameter ID.

#### Error codes
Error code          | Scope                 | Purpose
--------------------|-----------------------|------------------------------------
`ACCOUNT_NOT_FOUND` | securitiesAccount     | Securities account does not exist or does not belong to the user.


+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [SecuritiesAccount][]

### Update single Securities account of user [PUT]
This endpoint can be used to trigger the update of user-specific securities account settings. Currently only the alias can be changed. The existing alias will be removed if the given payload has an empty alias or no alias. (If no alias is specified explicitly the backend automatically returns the account name as alias, hence the alias can’t be really deleted but only set back to the account name).

Changing the alias need not to be signed. Hence currently this endpoint will always execute the change at once and the return-payload will contain signInfo.state with value "NONE".

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

A **SecuritiesAccount** resource with updated *alias* attribute. 

#### Reply
A **SecuritiesAccount** resource with updated details of one user Securities portfolio account identified by parameter ID.

**SignInfo** type with *state* and *signId*. If *state* is `OPEN`, then request needs signature flow, in which *signId* should be used. Otherwise, if *state* is `NONE` this means request is performed without signature process flow (signId is not provided).

Description of PUT resource attributes:

| Level | Attribute name       | Type/Enum               | Mand/Opt | Attribute description                 | Expected values/format                                   |
|-------|----------------------|-------------------------|----------|---------------------------------------|----------------------------------------------------------|
| 1     | securitiesAccount    | SecuritiesAccount       | M        | SecuritiesAccount object              |                                                          |
| 1     | signInfo             | SIGNINFO                | M        | SignInfo Details                      |                                                          |

#### Error codes
Error code          | Scope                 | Purpose
--------------------|-----------------------|------------------------------------
`ACCOUNT_NOT_FOUND` | securitiesAccount     | Securities account does not exist or does not belong to the user.
`FIELD_INVALID`     | accountno             | The given accountno in payload doesn’t contain account number.
`ID_MISMATCH`       | id                    | The given ID in payload doesn’t match to the ID in URI.
`FIELD_TOO_LONG`    | alias                 | Length of the provided alias is greater than 35.

+ Request (application/json)

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

    + Body

            {
                "id": "2f03203727596531303d17",
                "accountno": "AT642011120031692500",
                "description": "MAXIMILIAN MUSTERMANN1 TEST- ABT. SEC",
                "alias": "NEW ALIAS Maximilian Mustermann - Securities",
                "balance": {
                    "value": 729079,
                    "precision": 2,
                    "currency": "EUR"
                },
                "settlementAccount": {
                    "number": "20031693700",
                    "bankCode": "20111"
                }
            }

+ Response 200 (application/json)

    + Body

            {
                "securitiesAccount": {
                    "id": "2f03203727596531303d17",
                    "accountno": "AT642011120031692500",
                    "description": "MAXIMILIAN MUSTERMANN1 TEST- ABT. SEC",
                    "alias": "NEW ALIAS Maximilian Mustermann - Securities",
                    "balance": {
                        "value": 729079,
                        "precision": 2,
                        "currency": "EUR"
                    },
                    "settlementAccount": {
                        "number": "20031693700",
                        "bankCode": "20111"
                    },
                    "subSecAccounts": [
                        {
                            "id": "001",
                            "name": " Security sub account 001",
                            "titles": [
                                {
                                    "title": "RORENTO              EO 3",
                                    "isin": "ANN757371433",
                                    "numberOfShares": 5.40,
                                    "lastPrice": {
                                        "value": 538500,
                                        "precision": 4,
                                        "currency": "EUR"
                                    },
                                    "lastPriceDate": "2014-06-19T22:00:00Z",
                                    "performance": {
                                        "value": 1837,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "performancePercent": 6.74,
                                    "marketValue": {
                                        "value": 29079,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "totalIncome": {
                                        "value": 0,
                                        "precision": 3,
                                        "currency": "EUR"
                                    },
                                    "avgPurchasePrice": {
                                        "value": 494400,
                                        "precision": 4,
                                        "currency": "EUR"
                                    },
                                    "avgPurchaseValue": {
                                        "value": 1457400,
                                        "precision": 4,
                                        "currency": "EUR"
                                    },
                                    "stockExchange": "SIX SWISS EX",
                                    "securityIndication": "Fondsanbieter",
                                    "securityType": "FUND",
                                    "productGroup": "BONDS_AND_MORE",
                                    "flags": [
                                        "capitalGainTax"
                                    ]
                                },
                                {
                                    "title": "Erste Group EURO STOXX 50 Express-Bond 2019",
                                    "isin": "AT0000A188S7",
                                    "numberOfShares": 7.00,
                                    "lastPrice": {
                                        "value": 100000,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "lastPriceDate": "2014-06-24T22:00:00Z",
                                    "performance": {
                                        "value": 7037,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "performancePercent": 7.74,
                                    "marketValue": {
                                        "value": 700000,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "totalIncome": {
                                        "value": 0,
                                        "precision": 3,
                                        "currency": "EUR"
                                    },
                                    "avgPurchasePrice": {
                                        "value": 100000,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "avgPurchaseValue": {
                                        "value": 700000,
                                        "precision": 2,
                                        "currency": "EUR"
                                    },
                                    "stockExchange": "EURO STOXX 50 Index",
                                    "securityIndication": "Reference quote",
                                    "securityType": "BOND",
                                    "productGroup": "BONDS_AND_MORE",
                                    "flags": [
                                        "capitalGainTax"
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "signInfo": {
                    "state": "NONE"
                }
            }


## SecuritiesList [/netbanking/my/securities{?size,page}]
Resource Securities List represents collection of Securities portfolio accounts with subaccounts and linked securities titles to which authorized user has access.
This resource consists of paging attributes and array of *embedded* **SecuritiesAccount** resource items.

Description of **SecuritiesList** resource attributes: 

| Level | Attribute name     | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|--------------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber         | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount          | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage           | INTEGER          | O        | Page number of following page (provided only when exist)             |                          |
| 1     | pageSize           | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | securitiesAccounts | ARRAY of SecuritiesAccount | O     | Array of Securities accounts accessible by the user (could be empty) |                          |

+ Parameters
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "pageSize": 10,
                "securitiesAccounts": [
                    {
                        "id": "2f03203727596531303d17",
                        "accountno": "AT642011120031692500",
                        "description": "MAXIMILIAN MUSTERMANN1 TEST- ABT. SEC",
                        "alias": "NEW ALIAS Maximilian Mustermann - Securities",
                        "balance": {
                            "value": 729079,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "settlementAccount": {
                            "number": "20031693700",
                            "bankCode": "20111"
                        },
                        "subSecAccounts": [
                            {
                                "id": "001",
                                "name": " Security sub account 001",
                                "titles": [
                                    {
                                        "title": "RORENTO              EO 3",
                                        "isin": "ANN757371433",
                                        "numberOfShares": 5.40,
                                        "lastPrice": {
                                            "value": 538500,
                                            "precision": 4,
                                            "currency": "EUR"
                                        },
                                        "lastPriceDate": "2014-06-19T22:00:00Z",
                                        "performance": {
                                            "value": 1837,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "performancePercent": 6.74,
                                        "marketValue": {
                                            "value": 29079,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "totalIncome": {
                                            "value": 0,
                                            "precision": 3,
                                            "currency": "EUR"
                                        },
                                        "avgPurchasePrice": {
                                            "value": 494400,
                                            "precision": 4,
                                            "currency": "EUR"
                                        },
                                        "avgPurchaseValue": {
                                            "value": 1457400,
                                            "precision": 4,
                                            "currency": "EUR"
                                        },
                                        "stockExchange": "SIX SWISS EX",
                                        "securityIndication": "Fondsanbieter",
                                        "securityType": "FUND",
                                        "productGroup": "BONDS_AND_MORE",
                                        "flags": [
                                            "capitalGainTax"
                                        ]
                                    },
                                    {
                                        "title": "Erste Group EURO STOXX 50 Express-Bond 2019",
                                        "isin": "AT0000A188S7",
                                        "numberOfShares": 7.00,
                                        "lastPrice": {
                                            "value": 100000,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "lastPriceDate": "2014-06-24T22:00:00Z",
                                        "performance": {
                                            "value": 7037,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "performancePercent": 7.74,
                                        "marketValue": {
                                            "value": 700000,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "totalIncome": {
                                            "value": 0,
                                            "precision": 3,
                                            "currency": "EUR"
                                        },
                                        "avgPurchasePrice": {
                                            "value": 100000,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "avgPurchaseValue": {
                                            "value": 700000,
                                            "precision": 2,
                                            "currency": "EUR"
                                        },
                                        "stockExchange": "EURO STOXX 50 Index",
                                        "securityIndication": "Reference quote",
                                        "securityType": "BOND",
                                        "productGroup": "BONDS_AND_MORE",
                                        "flags": [
                                            "capitalGainTax"
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }

### Get a list of Securities accounts for current user [GET]
Get possibly empty list of all Securities investments this user owns. This call is paginated and sorted always by ID (newer securities accounts first).

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **SecuritiesList** with possibly empty (omitted) array of *embedded* **SecuritiesAccount** items. The most current rates available at the backend (about 15 minutes delay to real-time) are delivered and used for computation of balances.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [SecuritiesList][]


## SecuritiesHistoricalValues [/netbanking/my/securities/{id}/historical-values{?size,page,startDate,endDate,includeTitleValues}]
Resource Securities Historical Values represents collection of historical values of Securities portfolio account identified by ID. Historical values at the given dates could include also collection of the historical values of covered title positions.
This resource consists of paging attributes and array of historical value structures.

Description of **SecuritiesHistoricalValues** resource attributes: 

| Level | Attribute name      | Type/Enum | Mand/Opt | Attribute description                                                                          | Expected values/format                      |
|-------|---------------------|-----------|----------|------------------------------------------------------------------------------------------------|---------------------------------------------|
| 1     | pageNumber          | INTEGER   | M        | Page number of returned page, starting from 0 for the first page                               |                                             |
| 1     | pageCount           | INTEGER   | M        | Total number of pages of defined size                                                          |                                             |
| 1     | nextPage            | INTEGER   | O        | Page number of following page (provided only when exist)                                                                  |                                             |
| 1     | pageSize            | INTEGER   | M        | Provided or defaulted page size                                                                |                                             |
| 1     | historicalValues    | ARRAY of  | O        | Array of Securities historical values (at least one structure when starDate equals to endDate) |                                             |
| 2     | date                | DATE      | M        | Valuation date of the historical value                                                         | ISO date format: YYYY-MM-DD                 |
| 2     | balance             | AMOUNT    | M        | Balance of Securities portfolio account in particular valuation date (_embedded AMOUNT type)   | Fields value, precision, currency           |
| 2     | titles              | ARRAY of  | O        | Array of Securities Titles belonging to particular Sub account in particular valuation date    |                                             |
| 3     | title               | TEXT      | M        | Title name of securities investment                                                            |                                             |  
| 3     | isin                | TEXT      | M        | International Securities Identification Number (ISIN) uniquely identifies security.            | Its structure is defined in ISO 6166        |
| 3     | numberOfShares      | FLOAT     | M        | Number of securities in valuation date (_embedded AMOUNT type)                                 |                                             |
| 3     | lastPrice           | AMOUNT    | M        | Last Price of Securities title till valuation date (_embedded AMOUNT type)                     | Fields value, precision, currency           |
| 3     | lastPriceDate       | DATETIME  | M        | Date of securities last price evaluation till valuation date                                   | ISO dateTime format:  YYYY-MM-DDThh:mm:ssZ  |
| 3     | performance         | AMOUNT    | O        | Performance of Securities title till valuation date (_embedded AMOUNT type) *In what period?*  | Fields value, precision, currency           |
| 3     | performancePercent  | FLOAT     | O        | Performance of Securities title in percentage                                                  | Value in percentage, 1,5 means 1,5 %        |
| 3     | marketValue         | AMOUNT    | M        | Market Value of Securities title in valuation date (_embedded AMOUNT type)                     | Fields value, precision, currency           |
| 3     | totalIncome         | AMOUNT    | O        | Total Income of Securities title till valuation date (_embedded AMOUNT type) *In what period?* | Fields value, precision, currency           |
| 3     | avgPurchasePrice    | AMOUNT    | O        | Average purchase price of Securities title till valuation date (_embedded AMOUNT type)         | Fields value, precision, currency           |
| 3     | avgPurchaseValue    | AMOUNT    | O        | Average purchase value of Securities title till valuation date (_embedded AMOUNT type)         | Fields value, precision, currency           |

+ Parameters
    + id (TEXT, required) ... internal ID of the Securities portfolio account used as part of URI.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.
    + startDate (DATE, required) ... Start date of period for historical values query used as URI parameter. Mandatory request parameter with format: "YYYY-MM-DD".
    + endDate (DATE, required) ... End date of period for historical values query used as URI parameter. Mandatory request parameter with format: "YYYY-MM-DD".
    + includeTitleValues (BOOLEAN, optional) ... Filtering flag for including title information in response payload. Optional flag used as URI parameter, while "true" is used as default value (Titles will be included in response)
    
+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 10,
                "nextPage": 1,
                "pageSize": 100,
                "historicalValues": [
                    {
                        "date": "2014-01-15",
                        "balance": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "date": "2014-02-01",
                        "balance": {
                            "value": 0,
                            "precision": 2,
                            "currency": "EUR"
                        }
                    },
                    {
                        "date": "2014-03-01",
                        "balance": {
                            "value": 1313425,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "titles": [
                            {
                                "title": "ERSTE GROUP BK ST.AKT.ON",
                                "isin": "AT0000652011",
                                "numberOfShares": 500,
                                "lastPrice": {
                                    "value": 2571,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "lastPriceDate": "2014-02-27T23:00:00Z",
                                "marketValue": {
                                    "value": 1285500,
                                    "precision": 2,
                                    "currency": "EUR"
                                }
                            },
                            {
                                "title": "RORENTO              EO 3",
                                "isin": "ANN757371433",
                                "numberOfShares": 5,
                                "lastPrice": {
                                    "value": 5585,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "lastPriceDate": "2014-02-27T23:00:00Z",
                                "performance": {
                                    "value": 1837,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "performancePercent": 6.74,
                                "marketValue": {
                                    "value": 27925,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "totalIncome": {
                                    "value": 0,
                                    "precision": 3,
                                    "currency": "EUR"
                                },
                                "avgPurchasePrice": {
                                    "value": 5585,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "avgPurchaseValue": {
                                    "value": 27925,
                                    "precision": 2,
                                    "currency": "EUR"
                                }
                            }
                        ]
                    },
                    {
                        "date": "2014-03-18",
                        "balance": {
                            "value": 1200000,
                            "precision": 2,
                            "currency": "EUR"
                        },
                        "titles": [
                            {
                                "title": "ERSTE GROUP BK ST.AKT.ON",
                                "isin": "AT0000652011",
                                "numberOfShares": 500,
                                "lastPrice": {
                                    "value": 2400,
                                    "precision": 2,
                                    "currency": "EUR"
                                },
                                "lastPriceDate": "2014-03-17T23:00:00Z",
                                "marketValue": {
                                    "value": 1200000,
                                    "precision": 2,
                                    "currency": "EUR"
                                }
                            }
                        ]
                    }
                ]
            }

### Get a list of historical values for one Securities account [GET]
Get a list of historical values of one Securities portfolio account for a requested period of time (which must be in the past of course). The returned payload includes historical values for startDate, all 1st days of the months (that lay between startDate and endDate) and endDate. If startDate and endDate are the same, only one entry is returned in the payload. If there is no data available for a certain date, balance is returned as 0 for this date. 
This call is paginated and always sorted by *date*, no other sorts are supported due to performance reasons. 

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **SecuritiesHistoricalValues** with optional (omitted) array of title historical items.

#### Error codes
Error code      | Scope          | Purpose
----------------|----------------|------------------------------------
`ID_NOT_FOUND`  | id             | Securities account does not exist or does not belong to the user.
`FIELD_MISSING` | startDate      | No start date has been sent in mandatory request parameter.
`FIELD_MISSING` | andDate        | No end date has been sent in mandatory request parameter.
`FIELD_INVALID` | startDate      | Start date is not provided in the defined format (YYYY-MM-DD) or is an invalid date.
`FIELD_INVALID` | endDate        | End date is not provided in the defined format (YYYY-MM-DD) or is an invalid date.
`FIELD_INVALID` | startDate      | Start date is greater than end date.
`FIELD_INVALID` | startDate      | Start date is today or in future.
`FIELD_INVALID` | endDate        | End date is today or in future.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ
            

+ Response 200 (application/json)

    [SecuritiesHistoricalValues][]


# Group Info Services
Supporting info resources of *Info Services API*.


## Version [/info/version]
Version resource represents "debbuging" information about the current deployed software.

Description of **Version** resource attributes: 

| Level | Attribute name        | Type/Enum  | Mand/Opt | Attribute description                                                     | Expected values/format   |
|-------|-----------------------|------------|----------|---------------------------------------------------------------------------|--------------------------|
| 1     | version               | TEXT       | M        | Deployed software version, e.g. Maven information                         |                          |
| 1     | build                 | TEXT       | M        | Build server information, e.g. Build number                               |                          |
| 1     | revision              | TEXT       | O        | SVN information                                                           |                          |
| 1     | jobName               | TEXT       | O        | Build server information, e.g. Job name for release                       |                          |
| 1     | buildTag              | TEXT       | O        | Build server information, e.g. jenkins-jobName-build                      |                          |
| 1     | taggedVersion         | TEXT       | O        | Tag for release version                                                   |                          |
| 1     | buildDate             | TEXT       | M        | Build date of deployment of current version, e.g. YYYYMMDD_HHMM           |                          |
| 1     | apiGroupSpecVersion   | TEXT       | M        | George WebAPI Group Spec version which deployed software correspond to    |                          |
| 1     | signGroupSpecVersion  | TEXT       | M        | George Signing Group Spec version which deployed software correspond to   |                          |
| 1     | environment           | TEXT       | O        | Environment where deployed software is running                            |                          |

+ Model

    + Body

            {
                "version": "5.4.2",
                "build": "8",
                "revision": "18530",
                "jobName": "presto-2015S08-release",
                "buildTag": "jenkins-presto-2015S08-release-8",
                "taggedVersion": "rel_2015-S08-SP2",
                "buildDate": "20150824_0942",
                "apiGroupSpecVersion": "2.1",
                "signGroupSpecVersion": "2.0",
                "environment": "uat.sparkasse.at"
            }

### Get version information [GET]
Returns version information about the deployed software. 
This call could be used as health check (without need of authorization token) and also as debugging call to check version and build information.

#### Request
No parameters required

#### Reply
Resource **Version** with version and build information.

#### Error codes
No call-specific error codes.

+ Response 200 (application/json)

    [Version][]


## Locations [/info/locations{?countryIso,city,zipCode}]
Locations resource represents list of cities, locations which match search criteria (requested country, substring of city name, ZIP code).
This resource consists of paging attributes and array of **city** location items.

Description of **Locations** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page                                        |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | results        | ARRAY of         | O        | Array of city locations in particular country (could be empty)       |                          |
| 2     | city           | structure        | M        | Structure of city location                                           |                          |
| 3     | countryIso     | ISO3166          | M        | ISO 3166 ALPHA-2 code of country (e.g. AT)                           |                          |
| 3     | city           | TEXT             | M        | Name of city location                                                |                          |
| 3     | zipCode        | TEXT             | M        | Postal ZIP code of city location                                     |                          |

+ Parameters
    + countryIso (TEXT, required) ... ISO country code identifies for which country list of locations should be provided. Default value of this URI parameter is local country code. 
    + city (TEXT, optional) ... Substring of city name is used as URI parameter for filtering results.
    + zipCode (TEXT, optional) ... Postal ZIP code is used as URI parameter for filtering results.
    
+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "nextPage": 0,
                "pageSize": 20,
                "results": [
                    {
                        "city": {
                            "countryIso": "AT",
                            "zipCode": "1010",
                            "city": "Wien"
                        }
                    },
                    {
                        "city": {
                            "countryIso": "AT",
                            "zipCode": "4272",
                            "city": "Wienau"
                        }
                    },
                    {
                        "city": {
                            "countryIso": "AT",
                            "zipCode": "3643",
                            "city": "Wienau"
                        }
                    },
                    {
                        "city": {
                            "countryIso": "AT",
                            "zipCode": "9314",
                            "city": "Wiendorf"
                        }
                    },
                    {
                        "city": {
                            "countryIso": "AT",
                            "zipCode": "2351",
                            "city": "Wiener Neudorf"
                        }
                    }
                ]
            }

### Get list of matching city locations [GET]
Returns all available city locations matching the query parameters: country code and optional city name (substring search) and ZIP code. This call returns a list of maximum 100 city locations, even the searching criteria match more than 100 results.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **Locations** with list of available city locations based on searching parameters.

#### Error codes
Error code            | Scope                     | Purpose
--------------------- | ------------------------- | ------------------------------------------------
`DATA_NOT_AVAILABLE`  | countryIso, city, zipCode | No data was found for the combination of country/city/zipCode.
`VALUE_INVALID`       | countryIso                | ISO country code is invalid.           
`VALUE_INVALID`       | zipCode                   | Postal ZIP code is invalid for requested country.           

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Locations][]


## Country [/info/countries/{countryIso}]
Country resource consists of country ISO code, name of country, flag if country belongs to SEPA (Single Euro Payment Area) and local currency code used in country's clearing system.

Description of **Country** resource attributes: 

| Level | Attribute name | Type/Enum  | Mand/Opt | Attribute description                                                                    | Expected values/format           |
|-------|----------------|------------|----------|------------------------------------------------------------------------------------------|----------------------------------|
| 1     | countryIso     | ISO3166    | M        | ISO 3166 ALPHA-2 code of country (e.g. AT)                                               |                                  |
| 1     | name           | TEXT       | M        | Name of country                                                                          |                                  |
| 1     | isSepaCountry  | BOOLEAN    | O        | Flag indicates if country belongs to SEPA.                                               | Boolean values: `true`/`false`   |
| 1     | countryIban    | ISO3166    | O        | ISO 3166 ALPHA-2 code of country used in IBAN, must be provided for SEPA country. Usually countryIban equals to countryIso, only exceptions are former French colonies like French Guiana, Martinique etc, which have countryIban="FR".  |        |
| 1     | localCurrency  | ISO4217    | O        | ISO 4217 ALPHA-3 local currency code used in country, must be provided for SEPA country. |                                  |

+ Parameters
    + countryIso (TEXT, required) ... ISO country code identifies requested country. 

+ Model

    + Body

            {
                "countryIso": "AT",
                "name": "Austria",
                "isSepaCountry": "true",
                "countryIban": "AT",
                "localCurrency": "EUR"
            }

### Get country [GET]
Returns country code and name in requested language (accept-language in request, only languages supported by local API) and SEPA attributes.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **Country** with code, name, flag SEPA and local currency code used in country.

#### Error codes
Error code            | Scope                     | Purpose
--------------------- | ------------------------- | ------------------------------------------------
`VALUE_INVALID`       | countryIso                | ISO country code is invalid.           

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Country][]


## CountryList [/info/countries]
Country List resource represents list of countries codes, names and SEPA attributes. This resource consists of array of  *embedded* **Country** resource items.

Description of **Country List** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | countries      | ARRAY of Country | M        | Array of country structures (countryIso and name)                    |                          |

+ Model

    + Body

            {
                "countries": [
                    {
                        "countryIso": "AT",
                        "name": "Austria",
                        "isSepaCountry": "true",
                        "countryIban": "AT",
                        "localCurrency": "EUR"
                    },
                    {
                        "countryIso": "CZ",
                        "name": "Czech Republic",
                        "isSepaCountry": "true",
                        "countryIban": "CZ",
                        "localCurrency": "CZK"
                    },
                    {
                        "countryIso": "SK",
                        "name": "Slovak Republic",
                        "isSepaCountry": "true",
                        "countryIban": "SK",
                        "localCurrency": "EUR"
                    },
                    {
                        "countryIso": "UA",
                        "name": "Ukraine",
                        "isSepaCountry": "false"
                    },
                    {
                        "countryIso": "US",
                        "name": "United States",
                        "isSepaCountry": "false"
                    }
                ]
            }

### Get list of countries [GET]
Returns all countries with ISO codes and names in requested language (accept-language in request, only languages supported by local API) and SEPA attributes.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **CountryList** with list of all countries.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CountryList][]


## FinancialInstitution [/info/financialinstitution{?bankCode,bic}]
Financial Institution resource represents one financial institution identified by BIC or local bankCode (only for local clearing clients).

Description of **FinancialInstitution** resource attributes: 

| Level | Attribute name | Type/Enum     | Mand/Opt | Attribute description                                     | Expected values/format            |
|-------|----------------|---------------|----------|-----------------------------------------------------------|-----------------------------------|
| 1     | id             | TEXT          | M        | Internal ID of financial institution                      |                                   |
| 1     | name           | TEXT          | M        | Name of financial institution                             |                                   |
| 1     | streetNumber   | TEXT          | M        | Street name and number of financial institution address   |                                   |
| 1     | zipCodeCity    | TEXT          | M        | Postal ZIP code and city of financial institution address |                                   |
| 1     | countryCode    | ISO3166       | M        | ISO 3166 ALPHA-2 country code of financial institution    |                                   |
| 1     | isSepaBank     | BOOLEAN       | O        | Flag indicates if bank belongs to SEPA.                   | Boolean values: `true`/`false`    |

+ Parameters
    + bankCode (TEXT, optional) ... Local clearing bank code as URI parameter to identify financial institution. 
    + bic (TEXT, optional) ... BIC used in SWIFT transfers as URI parameter to identify financial institution. Only one of these two parameters must be provided as identificator.

+ Model

    + Body

            {
                "id": "1234567890123456",
                "name": "Testing Financial bank",
                "streetNumber": "Test street 23",
                "zipCodeCity": "Wien",
                "countryCode": "AT",
                "isSepaBank": "true"
            }

### Get financial institution [GET]
Returns financial institution information for the given bank code / BIC.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **FinancialInstitution** with name, address and SEPA flag.

#### Error codes
Error code      | Scope           | Purpose
--------------- | --------------- | ------------------------------------------------
`FIELD_MISING`  | bankCode, bic   | Either bankcode or BIC should be entered.
`INTEGRITY`     | bankCode, bic   | Either bankcode or BIC can be entered in the input parameters, but not both.          
`NOT_FOUND`     | bankCode        | There is no financial institution for the given bankcode.
`NOT_FOUND`     | bic             | There is no financial institution for the given BIC.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [FinancialInstitution][]


## FinancialInstitutionList [/info/financialinstitutions]
Financial Institution List resource represents list of country local financial institution codes (BIC or local clearing bankCode) and names.

Description of **FinancialInstitutionList** resource attributes: 

| Level | Attribute name | Type/Enum     | Mand/Opt | Attribute description                                     | Expected values/format   |
|-------|----------------|---------------|----------|-----------------------------------------------------------|--------------------------|
| 1     | banks          | ARRAY of      | M        | Array of country local financial institutions             |                          |
| 2     | bic            | BIC           | C        | BIC code (also know as SWIFT code) of local bank (BIC, bankCode - at least one value must be provided) | ISO 9362 format, 8 or 11 characters |
| 2     | bankCode       | BANKCODE      | C        | Bank code used in country local clearing system (BIC, bankCode - at least one value must be provided)  | local country format                |
| 2     | name           | TEXT          | M        | Official name of financial institution                    |                          |

+ Model

    + Body

            {
                "banks": [
                    {
                        "bankCode": "0100",
                        "name": "Komercní banka, a.s."
                    },
                    {
                        "bankCode": "0710",
                        "name": "Ceská národní banka"
                    },
                    {
                        "bankCode": "0800",
                        "name": "Ceská sporitelna, a.s."
                    }
                ]
            }

### Get list of local financial institutions [GET]
Returns all active local financial institution codes and names in requested language (accept-language in request, only languages supported by local API).

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **FinancialInstitutionList** with list of all local financial institutions.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [FinancialInstitutionList][]


## Institute [/info/institutes/{id}]
Institute resource represents one local Erste Bank institute identified by ID.

Description of **Institute** resource attributes: 

| Level | Attribute name | Type/Enum     | Mand/Opt | Attribute description                                                         | Expected values/format   |
|-------|----------------|---------------|----------|-------------------------------------------------------------------------------|--------------------------|
| 1     | number         | INTEGER       | M        | Internal local identification number of institute                             |                          |
| 1     | bankCode       | BANKCODE      | M        | Sort code number of local institute (bank code used in local clearing system) |                          |
| 1     | name           | TEXT          | M        | Name of local institute                                                       |                          |
| 1     | nameLong       | TEXT          | M        | Extended full name of local institute                                         |                          |

+ Parameters
    + id (TEXT) ... ID internal identifier of local institute used as part of URI.

+ Model

    + Body

            {
                "number": 198,
                "bankCode": 20111,
                "name": "Erste Bank",
                "nameLong": "Erste Bank der oesterreichischen Sparkassen AG"
            }

### Get local institute [GET]
Returns information about local institute identified by ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **Institute** with information about local institute.

#### Error codes
Error code                              | Scope    | Purpose
----------------------------------------|----------|------------------------------------
`ID_NOT_FOUND`                          | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Institute][]


## CurrencyExchangeRates [/info/fx/{currency}{?institute}]
Currency Exchange Rates resource represents foreign exchange rates (buy, sell and middle rate) of the requested currency with their precision (decimal places), denominations of bills (only which are traded, e.g. no 2 USD bill). The indicator signals whether a currency is moved up, down or stayed the same since the last change.

Description of **CurrencyExchangeRates** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                     | Expected values/format        |
|-------|----------------|------------------|----------|---------------------------------------------------------------------------|-------------------------------|
| 1     | currency       | ISO4217          | M        | ISO Currency code                                                         | E.g. `EUR`, `CZK`             |
| 1     | precision      | INTEGER          | M        | Number of decimal places for particular currency                          |                               |
| 1     | rateUnit       | INTEGER          | O        | Rate unit used for particular currency rates, when buy, middle, sell rate must be divided by rateUnit to get final rate  | Default value is 1, but could be 100 (e.g. for HUF, JPY, RUB) |
| 1     | nameI18N       | TEXT             | O        | Localized currency name in requested language                             |                               |
| 1     | denominations  | ARRAY of INTEGER | O        | List of available denominations of particular currency                    |                               |
| 1     | exchangeRate   | structure        | M        | Structure of exchange rates for particular currency (for non-cash)        |                               |
| 2     | buy            | FLOAT            | M        | Bank buy exchange rate (ratio to local currency)                          |                               |
| 2     | middle         | FLOAT            | M        | Middle exchange rate (ratio to local currency)                            |                               |
| 2     | sell           | FLOAT            | M        | Bank sell exchange rate (ratio to local currency)                         |                               |
| 2     | indicator      | INTEGER          | O        | Indicator if the rate was going up (1), down (-1) or stayed the same (0)  |                               |
| 2     | lastModified   | DATETIME         | M        | Datetime when rates were changed the last time                            |                               |
| 1     | banknoteRate   | structure        | O        | Structure of exchange rates for particular currency (for banknotes)       |                               |
| 2     | buy            | FLOAT            | M        | Bank buy exchange rate (ratio to local currency)                          |                               |
| 2     | middle         | FLOAT            | M        | Middle exchange rate (ratio to local currency)                            |                               |
| 2     | sell           | FLOAT            | M        | Bank sell exchange rate (ratio to local currency)                         |                               |
| 2     | indicator      | INTEGER          | O        | Indicator if the rate was going up (1), down (-1) or stayed the same (0)  |                               |
| 2     | lastModified   | DATETIME         | M        | Datetime when rates were changed the last time                            |                               |
| 1     | flags          | FLAGS            | O        | Array of optional flag values, the absence of a certain string is considered as “false” | FLAGS: `provided`,`tradingProhibited` |

The following flags can be applied to field *flags* in **CurrencyExchangeRates** resource:

Flag                            | Description
--------------------------------|-----------------------------------------------
`provided`                      | Flag indicating if current exchange rates for particular currency were provided by BE
`tradingProhibited`             | Flag whether buy/sell operations of particular currency is not allowed in bank
`internationalPaymentAllowed`   | Flag indicating if currency is allowed for international transfer from bank
`intraBankPaymentAllowed`       | Flag indicating if currency is allowed for internal intra bank transfer (between accounts of our local bank) 

+ Parameters
    + currency (INTEGER, required) ... Currency ISO code used as part of URI to identify requested currency for exchange rates.
    + institute (INTEGER, optional) ... Institute ID used as URI parameter. Institute ID identifies institute/branch with possible different FX rates in AT. If it is omitted, default value 198 is used in AT.

+ Model

    + Body

            {
                "currency" : "CHF",
                "precision" : 2,
                "rateUnit" : 1,
                "nameI18N" : "Swiss Franc",
                "denominations" : [ "1000", "200", "100", "50", "20", "10" ],
                "exchangeRate" : {
                    "buy" : 1.209,
                    "middle" : 1.2161,
                    "sell" : 1.2232,
                    "indicator": 1,
                    "lastModified" : "2014-12-16T23:00:00Z"
                },
                "banknoteRate" : {
                    "buy" : 1.194,
                    "middle" : 1.216,
                    "sell" : 1.238,
                    "lastModified" : "2014-12-15T23:00:00Z"
                },
                "flags" : [
                    "provided",
                    "internationalPaymentAllowed",
                    "intraBankPaymentAllowed"
                ]
            }


### Get one currency exchange rates [GET]
Returns the details of one specific currency foreign exchange rates.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **CurrencyExchangeRates** with the buy, sell and middle rate for a currency valid for whole local bank (or institute in AT). Also provides an indicator and the last modified time.

#### Error codes
Error code         | Scope                 | Purpose
------------------ | --------------------- | ------------------------------------------------
`NOT_FOUND`        | currency              | The provided currency is not used in local bank.
`ID_NOT_FOUND`     | institute             | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [CurrencyExchangeRates][]


## ExchangeRatesList [/info/fx{?institute}]
Exchange Rates List resource represents list of all available foreign exchange rates with their precision (decimal places), denominations of bills (only which are traded, e.g. no 2 USD bill). The indicator signals whether a currency is moved up, down or stayed the same since the last backend change.
This call supports last-modified/if-modified-since and the Accept-Language header. This resource consists of additional meta data attributes and array of *embedded* **ExchangeCurrencyRates** resource items.

Description of **ExchangeRatesList** resource attributes: 

| Level | Attribute name | Type/Enum         | Mand/Opt | Attribute description                                                                                  | Expected values/format        |
|-------|----------------|-------------------|----------|--------------------------------------------------------------------------------------------------------|-------------------------------|
| 1     | instituteId    | INTEGER           | O        | Institute ID (FX rates list could be defined for each institute/branch in AT, mandatory only for AT).  | Default value for AT: 198     |
| 1     | lastUpdated    | DATETIME          | M        | Datetime when FX rates list was updated for the last time.                                             | ISO Datetime format           |
| 1     | comparisonDate | DATETIME          | O        | Datetime of validity of the previous rates which are used to create the trend indicators.              | ISO Datetime format           |
| 1     | fx             | ARRAY of CurrencyExchangeRates | O        | Array of exchange rates for all available currencies (embedded CurrencyExchangeRates resource)       |                               |

+ Parameters
    + institute (INTEGER, optional) ... Institute ID used as URI parameter. Institute ID identifies institute/branch, with possible different FX rates in AT.  If it is omitted, default value 198 is used in AT.

+ Model

    + Body

            {
                "instituteId": 198,
                "lastUpdated": "2014-03-11T23:00:00Z",
                "comparisonDate": "2014-03-10T23:00:00Z",
                "fx" : [
                    {
                        "currency" : "USD",
                        "precision" : 2,
                        "rateUnit" : 1,
                        "nameI18N" : "US Dollar",
                        "denominations" : [ "100", "50", "20", "10", "5", "1" ],
                        "exchangeRate" : {
                            "buy" : 1.3817,
                            "middle" : 1.3867,
                            "sell" : 1.3917,
                            "indicator": 1,
                            "lastModified" : "2014-12-16T23:00:00Z"
                        },
                        "banknoteRate" : {
                            "buy" : 1.369,
                            "middle" : 1.387,
                            "sell" : 1.405,
                            "lastModified" : "2014-12-15T23:00:00Z"
                        },
                        "flags" : [
                            "provided",
                            "internationalPaymentAllowed",
                            "intraBankPaymentAllowed"
                        ]
                    },
                    {
                        "currency" : "CHF",
                        "precision" : 2,
                        "rateUnit" : 1,
                        "nameI18N" : "Swiss Franc",
                        "denominations" : [ "1000", "200", "100", "50", "20", "10" ],
                        "exchangeRate" : {
                            "buy" : 1.209,
                            "middle" : 1.2161,
                            "sell" : 1.2232,
                            "indicator": 1,
                            "lastModified" : "2014-12-16T23:00:00Z"
                        },
                        "banknoteRate" : {
                            "buy" : 1.194,
                            "middle" : 1.216,
                            "sell" : 1.238,
                            "lastModified" : "2014-12-15T23:00:00Z"
                        },
                        "flags" : [
                            "provided",
                            "internationalPaymentAllowed",
                            "intraBankPaymentAllowed"
                        ]
                    },
                    {
                        "currency" : "PEN",
                        "precision" : 2,
                        "rateUnit" : 1,
                        "nameI18N" : "Peruvian Sol",
                        "exchangeRate" : {
                            "buy" : 3.3878,
                            "middle" : 3.8878,
                            "sell" : 4.3878,
                            "lastModified" : "2014-12-16T23:00:00Z"
                        },
                        "flags" : [ "tradingProhibited" ]
                    }
                ]
            }

### Get a list of exchange rates [GET]
Returns list of all available foreign currencies exchange rates. This call supports last-modified/if-modified-since and the Accept-Language header.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **ExchangeRatesList** with list of FX exchange rates valid for whole local bank (or institute in AT).

#### Error codes
Error code            | Scope                 | Purpose
--------------------- | --------------------- | ------------------------------------------------
`ID_NOT_FOUND`        | institute             | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [ExchangeRatesList][]


## BankHolidays [/info/holidays/bank{?countryIso,startDate,size,page}]
Bank Holidays resource represents list of non-banking days in requested country.
This resource consists of paging attributes and array of *embedded* **DATE** base type items.

Description of **BankHolidays** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------|--------------------------|
| 1     | pageNumber     | INTEGER          | M        | Page number of returned page, starting from 0 for the first page     |                          |
| 1     | pageCount      | INTEGER          | M        | Total number of pages of defined size                                |                          |
| 1     | nextPage       | INTEGER          | O        | Page number of following page                                        |                          |
| 1     | pageSize       | INTEGER          | M        | Provided or defaulted page size                                      |                          |
| 1     | holidays       | ARRAY of DATE    | O        | Array of bank holidays in particular country (could be empty)        |                          |

+ Parameters
    + countryIso (TEXT, required) ... ISO country code identifies for which country list of bank holidays should be provided. Default value of this URI parameter is local country code. 
    + startDate (DATE, optional) ... List of bank holidays in the next 90 days from this given date and the selected country is provided. If not supplied all available bank holiday data for the specified country is returned, past and future dates.
    + size (INTEGER, optional) ... Page size used as URI parameter. There is no predefined size limit. If it is omitted, all records are returned in one large list.
    + page (INTEGER, optional) ... Requested page number used as URI parameter. Page count starts at zero, so 0 is the first page. If size is given without any page, page=0 is assumed as default.

+ Model

    + Body

            {
                "pageNumber": 0,
                "pageCount": 1,
                "nextPage": 0,
                "pageSize": 20,
                "holidays": [
                    "2014-06-20",
                    "2014-06-21"
                ]
            }

### Get bank holidays [GET]
Returns all available bank holidays stored in BE for the given country code and optional startDate.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **BankHolidays** with list of non-banking days in requested/local country.

#### Error codes
Error code            | Scope                 | Purpose
--------------------- | --------------------- | ------------------------------------------------
`DATA_NOT_AVAILABLE`  | countryIso, startDate | No data was found for the combination of country/startDate.
`FIELD_INVALID`       | countryIso            | ISO country code is invalid.           
`FIELD_INVALID`       | startDate             | The start date is invalid.           

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [BankHolidays][]


## Branch [/info/branches/{id}]
Branch resource represents detail of one existing branch of the local bank identified by ID.

Description of **Branch** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                      | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------------|--------------------------|
| 1     | id             | TEXT             | M        | ID identificator of branch                                                 |                          |
| 1     | description    | TEXT             | O        | Name, description of branch                                                |                          |
| 1     | address        | structure        | O        | Address structure of branch                                                |                          |
| 2     | street         | TEXT             | O        | Branch address street and street number, building number                   |                          |
| 2     | city           | TEXT             | M        | Branch city/village name with optional county/region or city district      |                          |
| 2     | zipCode        | TEXT             | M        | Postal ZIP Code                                                            |                          |
| 2     | country        | ISO3166          | O        | ISO 3166 ALPHA-2 code of country                                           |                          |
| 1     | location       | structure        | O        | Location structure of branch                                               |                          |
| 2     | latitude       | FLOAT            | M        | Branch Location latitude                                                   |                          |
| 2     | longitude      | FLOAT            | M        | Branch Location longitude                                                  |                          |
| 1     | email          | EMAIL            | O        | Contact email address of branch                                            | E-mail address (pattern “[A-Za-z0–9@_. -]+$”, length min 5, max 50) |
| 1     | phone          | TEXT             | O        | Contact phone (country, area calling prefix and phone number) of branch    |                          |
    

+ Parameters
    + id (TEXT, required) ... ID internal identifier of local branch used as part of URI.

+ Model

    + Body

            {
                "id": "7",
                "description": "Filialka VŠE Praha",
                "address": {
                    "street": "námestí Winstona Churchilla 1938/4",
                    "city": "Praha 3",
                    "zipCode": "13000"
                },
                "location": {
                    "latitude": 51.10648772767337,
                    "longitude": 15.447021484375
                },
                "phone": "956720560"
            }

### Get bank branch detail [GET]
Returns detail of bank branch of local bank identified by ID.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **Branch** with detail of existing branch in local country.

#### Error codes
Error code                              | Scope    | Purpose
----------------------------------------|----------|------------------------------------
`ID_NOT_FOUND`                          | id       | The provided ID does not exist.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [Branch][]
    
    
## BranchList [/info/branches{?matching}]
Branch List resource represents list of all existing branches of the local bank.

Description of **BranchList** resource attributes: 

| Level | Attribute name | Type/Enum        | Mand/Opt | Attribute description                                                      | Expected values/format   |
|-------|----------------|------------------|----------|----------------------------------------------------------------------------|--------------------------|
| 1     | branches       | ARRAY of Branch  | O        | Array of bank branches in local country                                    |                          |

+ Parameters
    + matching (TEXT, optional) ... String to filter branches, matching string should be in some of branch fields: description, street, city

+ Model

    + Body

            {
                "branches": [
                    {
                        "id": "7",
                        "description": "Filialka VŠE Praha",
                        "address": {
                            "street": "námestí Winstona Churchilla 1938/4",
                            "city": "Praha 3",
                            "zipCode": "13000"
                        },
                        "location": {
                            "latitude": 51.10648772767337,
                            "longitude": 15.447021484375
                        },
                        "phone": "956720560"
                    },
                    {
                        "id": "9",
                        "description": "Hypotecní centrum Praha 1",
                        "address": {
                            "street": "Rytírská 536/29",
                            "city": "Praha 1",
                            "zipCode": "11000"
                        },
                        "location": {
                            "latitude": 51.1067337,
                            "longitude": 15.470214
                        },
                        "phone": "(02)956720786"
                    }
                ]
            }

### Get bank branches [GET]
Returns all available bank branches for the filtering optional matching string.

#### Request
**Authorization**: Authorization token (OAuth 2.0 Bearer - Base64 encoded HTTP header field) provided by Federated Login solution.

#### Reply
Resource **BranchList** with list of existing branches in local country.

#### Error codes
No call-specific error codes.

+ Request

    + Headers

            Authorization: Bearer ya29.AHES67zeEn-RDg9CA5gGKMLKuG4uVB7W4O4WjNr-NBfY6Dtad4vbIZ

+ Response 200 (application/json)

    [BranchList][]