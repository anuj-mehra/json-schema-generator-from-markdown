FORMAT: 1A
HOST: https://www.localbank.eu/webapi/environment/version/

# Sample Markdown File

# WebAPI Basics

## Server calls

Server calls are done via *https* by using one of the following HTTP methods:

Method  | Idempotent  | Change state  | Purpose
------- | ----------- | ------------- | ------------------------------------------------------------------
GET     | yes         | no            | select: read-only call (e.g. get account list)
POST    | no          | yes           | create: add a new resource (e.g. create a new transaction), call a function
PUT     | yes         | yes           | update: change an existing resource (e.g. set a message read, lock a card)
DELETE  | yes         | yes           | delete: remove an existing resource (e.g. delete a message)


### Authentication and Authorization



#### Authentication in file downloads in old browsers


### JSON

In case of POST / PUT / DELETE requests business data is accepted as JSON data (if it is not binary data). See http://en.wikipedia.org/wiki/JSON for details on JSON.

Empty arrays `[]` and `null` values must be omitted from input data (request payload).


## Server reply

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
        "acctkey": 5,
        "acctCodes": [ 7, 8, 9 ],
        "trankey": "100001",
        "trandate": "23-MAY-2023",
        "flags": [ ... array ... ]
    }


### Error codes


## API URLs

Prefixes in URL are used to differentiate the environment version of API. Every local country implementation should define it as following example from AT:

 ENV   | HOST
------ | ----------------------------------------------
 ENTW  | tbd
 FAT   | https://api.fat.sparkasse.at/rest/
 UAT   | https://api.uat.sparkasse.at/rest/
 PROD  | tbd


## API Calls


## API Versioning


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


## DataType:Float (FLOAT)

The float type is as four-byte, single-precision, floating-point number. It represents a single-precision 32-bit IEEE 754 value. 
The float type can represent numbers as large as 3.4E+38 (positive or negative) with an accuracy of about seven digits, and as small as 1E-44.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern |            Constant                 |
|-------|-------------------------|-------------|----------|----------|-------------------------------------|
| 0     | FLOAT                   | number      |          |          |                                     |


## DataType:Boolean (BOOLEAN)

The boolean type has just two values: `true` or `false`.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern                                   |            Constant                 |
|-------|-------------------------|-------------|----------|--------------------------------------------|-------------------------------------|
| 0     | BOOLEAN                 | boolean     |          |                                            |                                     |


## DataType:Universally unique identifier (UUID)

A 36-digit string containing a standard [universally unique identifier](http://en.wikipedia.org/wiki/Universally_unique_identifier).

    "uuid": "550e8400-e29b-41d4-a716-446655440000"

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | UUID                    | TEXT36      |          |                        |                                     |


## DataType:Currency (ISO4217)

Currency is in ISO 4217 format (3 capital letters code).

    {
        "currency":"EUR"           // (ISO4217)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ISO4217                 | TEXT3       |          | [A-Z][A-Z][A-Z]        |                                     |


## DataType:Country Codes (ISO3166)

Country codes are in ISO 3166-1 format, subtype ALPHA-2. This means two letters in uppercase. 

    {
        "country":"AT"           // (ISO3166)
    }

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ISO3166                 | TEXT2       |          | [A-Z][A-Z]             |                                     |


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


## DataType:Enums (ENUM)

Variables where the actual value is one of a predefined list (a.k.a. enums). The range of values are given as comma-separated list right in the datatype definition, e.g. (ENUM:GI,WP,SP,KA). Domain values are keys used in Domain Database, that provides company-wide reusable keys, short- and long-texts for them.

    "some_domain_value":"SP"     // (ENUM:GI,WP,SP,KA)

| Level | Datatype name           | Type        | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|-------------|----------|------------------------|-------------------------------------|
| 1     | ENUM                    | TEXT        |          |                        |                                     |


## DataType:Flags (FLAGS)

Flags/Tags/Roles that an object can have. To avoid adding a lot of case-specific boolean values, things (that an object is or not) are represented by flagging the object. Flags are an array of strings representing one aspect, e.g. one permission, one attribute or one role. The existence of a certain string in a Flag-List can be considered to be a "true" on this aspect, the absence of a certain string as a "false". The possible flags are listed on a case-by-case basis at each data type/call as comma-separated list within the brackets. 

    "flags": [
        "hidden", "owner"   // (FLAGS:hidden,owner,unused1,unused2)
    ]

Empty flag arrays must be omitted.

| Level | Datatype name           | Type                 | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|----------------------|----------|------------------------|-------------------------------------|
| 1     | FLAGS                   | ARRAY of TEXT        |          |                        |                                     |


## DataType:Features (FEATURES)

Features that an object can have or is capable of. To avoid adding a lot of case-specific attributes, Features are an array of strings representing one aspect, e.g. one feature of object. The existence of a certain string in a Feature-List can be used on FE to allow some functionality for object. The possible features are listed on a case-by-case basis at each data type/call as comma-separated list within the brackets. 

| Level | Datatype name           | Type                 | Mand/Opt |        Pattern         |            Constant                 |
|-------|-------------------------|----------------------|----------|------------------------|-------------------------------------|
| 1     | FEATURES                | ARRAY of TEXT        |          |                        |                                     |


## DataType:International bank account numbers (IBAN)

Based on ISO 13616-1:2007. A valid IBAN consists of all three of the following components: Country Code (2 capital letters), check digits (2 digits) and BBAN (local Basic Bank Account Number consisting of 1-30 characters). 

    "iban": "AT896000000005544815"

| Level | Datatype name           | Type        | Mand/Opt |        Pattern                          |            Constant                 |
|-------|-------------------------|-------------|----------|-----------------------------------------|-------------------------------------|
| 1     | IBAN                    | TEXT        |          |  [A-Z]{2}[0-9]{2}[0-9a-zA-Z]{1,30}      |                                     |


## DataType:Bank Code (BANKCODE)

Local bank code used in local bank clearing system, e.g. 5-digit bank code in AT, 4-digit bank code in CZ, SK.

    "bankCode": "20111"     //Erste Bank der oesterreichischen Sparkassen AG
    "bankCode": "0800"      //Ceská sporitelna, a.s.
    "bankCode": "0900"      //Slovenská sporitelna, a.s.

| Level | Datatype name           | Type        | Mand/Opt |  Pattern      |            Constant                 |
|-------|-------------------------|-------------|----------|---------------|-------------------------------------|
| 1     | BANKCODE                | TEXT        |          | [0-9]{4,5}    |                                     |


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

