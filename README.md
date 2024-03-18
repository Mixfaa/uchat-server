Hardfork with mongodb as database
Also it is reactive (inside)

Unfortunately, reactive driver for mongodb does not support dbref

Uses encryption by this scheme:

User register with public key and stores his private key ->
Creates chat with other participants, sends them symmetric key encrypted with their public keys ->
Messages in chats are encrypted with this symmetric key 
