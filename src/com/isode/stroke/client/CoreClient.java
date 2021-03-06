/*
 * Copyright (c) 2010-2016, Isode Limited, London, England.
 * All rights reserved.
 */
package com.isode.stroke.client;

import com.isode.stroke.base.NotNull;
import com.isode.stroke.base.SafeByteArray;
import com.isode.stroke.elements.Message;
import com.isode.stroke.elements.Presence;
import com.isode.stroke.elements.Stanza;
import com.isode.stroke.elements.StreamType;
import com.isode.stroke.jid.JID;
import com.isode.stroke.crypto.JavaCryptoProvider;
import com.isode.stroke.idn.ICUConverter;
import com.isode.stroke.network.Connection;
import com.isode.stroke.network.ConnectionFactory;
import com.isode.stroke.network.Connector;
import com.isode.stroke.network.DomainNameResolveError;
import com.isode.stroke.network.NetworkFactories;
import com.isode.stroke.network.HostAddressPort;
import com.isode.stroke.network.SOCKS5ProxiedConnectionFactory;
import com.isode.stroke.network.HTTPConnectProxiedConnectionFactory;
import com.isode.stroke.parser.payloadparsers.FullPayloadParserFactoryCollection;
import com.isode.stroke.queries.IQRouter;
import com.isode.stroke.serializer.payloadserializers.FullPayloadSerializerCollection;
import com.isode.stroke.session.BasicSessionStream;
import com.isode.stroke.session.SessionStream;
import com.isode.stroke.signals.Signal;
import com.isode.stroke.signals.Signal1;
import com.isode.stroke.signals.SignalConnection;
import com.isode.stroke.signals.Slot;
import com.isode.stroke.signals.Slot1;
import com.isode.stroke.signals.Slot2;
import com.isode.stroke.tls.CertificateTrustChecker;
import com.isode.stroke.tls.CertificateVerificationError;
import com.isode.stroke.tls.CertificateWithKey;
import com.isode.stroke.tls.TLSOptions;
import com.isode.stroke.tls.TLSError;
import java.util.logging.Logger;
import java.util.Vector;

/**
 * The central class for communicating with an XMPP server.
 *
 * This class is responsible for setting up the connection with the XMPP server, authenticating, and
 * initializing the session.
 *
 * This class can be used directly in your application, although the Client subclass provides more
 * functionality and interfaces, and is better suited for most needs.
 */
public class CoreClient {
    /**
     * The user should add a listener to this signal, which will be called when
     * the client was disconnected from tne network.
     *
     * <p>If the disconnection was due to a non-recoverable error, the type
     * of error will be passed as a parameter.
     */
    public final Signal1<ClientError> onDisconnected = new Signal1<ClientError>();

    /**
     * The user should add a listener to this signal, which will be called when
     * the connection is established with the server.
     */
    public final Signal onConnected = new Signal();

    /**
     * The user may add a listener to this signal, which will be called when
     * data are received from the server. Useful for observing protocol exchange.
     */
    public final Signal1<SafeByteArray> onDataRead = new Signal1<SafeByteArray>();

    /**
     * The user may add a listener to this signal, which will be called when
     * data are sent to the server. Useful for observing protocol exchange.
     */
    public final Signal1<SafeByteArray> onDataWritten = new Signal1<SafeByteArray>();

    /**
     * Called when a message stanza is received.
     */
    public final Signal1<Message> onMessageReceived = new Signal1<Message>();
    /**
     * Called when a presence stanza is received.
     */
    public final Signal1<Presence> onPresenceReceived = new Signal1<Presence>();
    /**
     * Called when a stanza has been received and acked by a server supporting XEP-0198.
     */
    public final Signal1<Stanza> onStanzaAcked = new Signal1<Stanza>();
    private JID jid_;
    private SafeByteArray password_;
    private ClientSessionStanzaChannel stanzaChannel_;
    private IQRouter iqRouter_;
    private Connector connector_;
    private FullPayloadParserFactoryCollection payloadParserFactories_ = new FullPayloadParserFactoryCollection();
    private FullPayloadSerializerCollection payloadSerializers_ = new FullPayloadSerializerCollection();
    private Connection connection_;
    private BasicSessionStream sessionStream_;
    private ClientSession session_;
    private CertificateWithKey certificate_;
    private boolean disconnectRequested_;
    private ClientOptions options;
    private CertificateTrustChecker certificateTrustChecker;
    private NetworkFactories networkFactories;
    private SignalConnection sessionStreamDataReadConnection_;
    private SignalConnection sessionStreamDataWrittenConnection_;
    private SignalConnection sessionFinishedConnection_;
    private SignalConnection sessionNeedCredentialsConnection_;
    private SignalConnection connectorConnectFinishedConnection_;
    private SignalConnection onMessageReceivedConnection;
    private SignalConnection onPresenceReceivedConnection;
    private SignalConnection onStanzaAckedConnection;
    private SignalConnection onAvailableChangedConnection;
    private Vector<ConnectionFactory> proxyConnectionFactories = new Vector<ConnectionFactory>();
    private Logger logger_ = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor.
     * 
     * @param eventLoop Event loop used by the class, must not be null. The
     *            CoreClient creates threads to do certain tasks. However, it
     *            posts events that it expects to be done in the application's
     *            main thread to this eventLoop. The application should
     *            use an appropriate EventLoop implementation for the application type. This
     *            EventLoop is just a way for the CoreClient to pass these
     *            events back to the main thread, and should not be used by the
     *            application for its own purposes.
     * @param jid User JID used to connect to the server, must not be null
     * @param password User password to use, must not be null
     * @param networkFactories An implementation of network interaction, must
     *            not be null.
     */
    public CoreClient(final JID jid, final SafeByteArray password, final NetworkFactories networkFactories) {
        jid_ = jid;
        password_ = password;
        disconnectRequested_ = false;
        this.networkFactories = networkFactories;
        this.certificateTrustChecker = null;
        stanzaChannel_ = new ClientSessionStanzaChannel();
        onMessageReceivedConnection = stanzaChannel_.onMessageReceived.connect(new Slot1<Message>() {

            public void call(Message p1) {
                handleMessageReceived(p1);
            }
        });
        onPresenceReceivedConnection = stanzaChannel_.onPresenceReceived.connect(new Slot1<Presence>() {

            public void call(Presence p1) {
                handlePresenceReceived(p1);
            }
        });
        onStanzaAckedConnection = stanzaChannel_.onStanzaAcked.connect(new Slot1<Stanza>() {

            public void call(Stanza p1) {
                handleStanzaAcked(p1);
            }
        });
        onAvailableChangedConnection = stanzaChannel_.onAvailableChanged.connect(new Slot1<Boolean>() {

            public void call(Boolean p1) {
                handleStanzaChannelAvailableChanged(p1);
            }
        });

        iqRouter_ = new IQRouter(stanzaChannel_);
        iqRouter_.setJID(jid);
    }

    protected void finalize() throws Throwable {
        try {
            forceReset();
            onAvailableChangedConnection.disconnect();
            onMessageReceivedConnection.disconnect();
            onPresenceReceivedConnection.disconnect();
            onStanzaAckedConnection.disconnect();
            stanzaChannel_ = null;
        }
        finally {
            super.finalize();
        }
    }

    /**
    * Connects the client to the server.
    *
    * After the connection is established, the client will set 
    * initialize the stream and authenticate.
    */
    public void connect() {
        connect(new ClientOptions());
    }

    /**
     * Connect using the standard XMPP connection rules (i.e. SRV then A/AAAA).
     * 
     * @param o Client options to use in the connection, must not be null
     */
    public void connect(final ClientOptions o) {
        logger_.fine("Connecting ");
        forceReset();
        disconnectRequested_ = false;
        assert (connector_ == null);
        options = o;

        // Determine connection types to use
        assert(proxyConnectionFactories.isEmpty());
        boolean useDirectConnection = true;
        HostAddressPort systemSOCKS5Proxy = networkFactories.getProxyProvider().getSOCKS5Proxy();
        HostAddressPort systemHTTPConnectProxy = networkFactories.getProxyProvider().getHTTPConnectProxy();
        switch (o.proxyType) {
            case NoProxy:
                logger_.fine(" without a proxy\n");
                break;
            case SystemConfiguredProxy:
                logger_.fine(" with a system configured proxy\n");
                if (systemSOCKS5Proxy.isValid()) {
                    logger_.fine("Found SOCK5 Proxy: " + systemSOCKS5Proxy.getAddress().toString() + ":" + systemSOCKS5Proxy.getPort() + "\n");
                    proxyConnectionFactories.add(new SOCKS5ProxiedConnectionFactory(networkFactories.getDomainNameResolver(), networkFactories.getConnectionFactory(), networkFactories.getTimerFactory(), systemSOCKS5Proxy.getAddress().toString(), systemSOCKS5Proxy.getPort()));
                }
                if (systemHTTPConnectProxy.isValid()) {
                    logger_.fine("Found HTTPConnect Proxy: " + systemHTTPConnectProxy.getAddress().toString() + ":" + systemHTTPConnectProxy.getPort() + "\n");
                    proxyConnectionFactories.add(new HTTPConnectProxiedConnectionFactory(networkFactories.getDomainNameResolver(), networkFactories.getConnectionFactory(), networkFactories.getTimerFactory(), systemHTTPConnectProxy.getAddress().toString(), systemHTTPConnectProxy.getPort()));
                }
                break;
            case SOCKS5Proxy: {
                logger_.fine(" with manual configured SOCKS5 proxy\n");
                String proxyHostname = o.manualProxyHostname.isEmpty() ? systemSOCKS5Proxy.getAddress().toString() : o.manualProxyHostname;
                int proxyPort = o.manualProxyPort == -1 ? systemSOCKS5Proxy.getPort() : o.manualProxyPort;
                logger_.fine("Proxy: " + proxyHostname + ":" + proxyPort + "\n");
                proxyConnectionFactories.add(new SOCKS5ProxiedConnectionFactory(networkFactories.getDomainNameResolver(), networkFactories.getConnectionFactory(), networkFactories.getTimerFactory(), proxyHostname, proxyPort));
                useDirectConnection = false;
                break;
            }
            case HTTPConnectProxy: {
                logger_.fine(" with manual configured HTTPConnect proxy\n");
                String proxyHostname = o.manualProxyHostname.isEmpty() ? systemHTTPConnectProxy.getAddress().toString() : o.manualProxyHostname;
                int proxyPort = o.manualProxyPort == -1 ? systemHTTPConnectProxy.getPort() : o.manualProxyPort;
                logger_.fine("Proxy: " + proxyHostname + ":" + proxyPort + "\n");
                proxyConnectionFactories.add(new HTTPConnectProxiedConnectionFactory(networkFactories.getDomainNameResolver(), networkFactories.getConnectionFactory(), networkFactories.getTimerFactory(), proxyHostname, proxyPort, o.httpTrafficFilter));
                useDirectConnection = false;
                break;
            }
        }
        Vector<ConnectionFactory> connectionFactories = new Vector<ConnectionFactory>(proxyConnectionFactories);
        if (useDirectConnection) {
            connectionFactories.add(networkFactories.getConnectionFactory());
        }

        String host = (o.manualHostname == null || o.manualHostname.isEmpty()) ? jid_.getDomain() : o.manualHostname;
        int port = o.manualPort;
        String serviceLookupPrefix = (o.manualHostname == null || o.manualHostname.isEmpty() ? "_xmpp-client._tcp." : null);
        assert(connector_ == null);
        //TO PORT
        /*if (options.boshURL.isEmpty()) {
            connector_ = new ChainedConnector(host, port, serviceLookupPrefix, networkFactories.getDomainNameResolver(), connectionFactories, networkFactories.getTimerFactory());
            connector_.onConnectFinished.connect(boost::bind(&CoreClient::handleConnectorFinished, this, _1, _2));
            connector_.setTimeoutMilliseconds(2*60*1000);
            connector_.start();
        }
        else {
            /* Autodiscovery of which proxy works is largely ok with a TCP session, because this is a one-off. With BOSH
             * it would be quite painful given that potentially every stanza could be sent on a new connection.
             */
            //sessionStream_ = boost::make_shared<BOSHSessionStream>(boost::make_shared<BOSHConnectionFactory>(options.boshURL, networkFactories.getConnectionFactory(), networkFactories.getXMLParserFactory(), networkFactories.getTLSContextFactory()), getPayloadParserFactories(), getPayloadSerializers(), networkFactories.getTLSContextFactory(), networkFactories.getTimerFactory(), networkFactories.getXMLParserFactory(), networkFactories.getEventLoop(), host, options.boshHTTPConnectProxyURL, options.boshHTTPConnectProxyAuthID, options.boshHTTPConnectProxyAuthPassword);
            /*sessionStream_ = boost::shared_ptr<BOSHSessionStream>(new BOSHSessionStream(
                    options.boshURL,
                    getPayloadParserFactories(),
                    getPayloadSerializers(),
                    networkFactories.getConnectionFactory(),
                    networkFactories.getTLSContextFactory(),
                    networkFactories.getTimerFactory(),
                    networkFactories.getXMLParserFactory(),
                    networkFactories.getEventLoop(),
                    networkFactories.getDomainNameResolver(),
                    host,
                    options.boshHTTPConnectProxyURL,
                    options.boshHTTPConnectProxyAuthID,
                    options.boshHTTPConnectProxyAuthPassword,
                    options.tlsOptions));
            sessionStream_.onDataRead.connect(boost::bind(&CoreClient::handleDataRead, this, _1));
            sessionStream_.onDataWritten.connect(boost::bind(&CoreClient::handleDataWritten, this, _1));
            bindSessionToStream();
        }*/
        connector_ = Connector.create(host, port, serviceLookupPrefix, networkFactories.getDomainNameResolver(), networkFactories.getConnectionFactory(), networkFactories.getTimerFactory());
        connectorConnectFinishedConnection_ = connector_.onConnectFinished.connect(new Slot2<Connection, com.isode.stroke.base.Error>() {
            public void call(Connection p1, com.isode.stroke.base.Error p2) {
                handleConnectorFinished(p1, p2);
            }
        });
        connector_.setTimeoutMilliseconds(2*60*1000);
        connector_.start();
    }

    private void bindSessionToStream() {
        session_ = ClientSession.create(jid_, sessionStream_, networkFactories.getIDNConverter(), networkFactories.getCryptoProvider());
        session_.setCertificateTrustChecker(certificateTrustChecker);
        session_.setUseStreamCompression(options.useStreamCompression);
        session_.setAllowPLAINOverNonTLS(options.allowPLAINWithoutTLS);
        session_.setSingleSignOn(options.singleSignOn);
        session_.setAuthenticationPort(options.manualPort);        
        switch (options.useTLS) {
            case UseTLSWhenAvailable:
                session_.setUseTLS(ClientSession.UseTLS.UseTLSWhenAvailable);
                break;
            case NeverUseTLS:
                session_.setUseTLS(ClientSession.UseTLS.NeverUseTLS);
                break;
            case RequireTLS:
                session_.setUseTLS(ClientSession.UseTLS.RequireTLS);
                break;
        }
        session_.setUseAcks(options.useAcks);
        stanzaChannel_.setSession(session_);
        sessionFinishedConnection_ = session_.onFinished.connect(new Slot1<com.isode.stroke.base.Error>() {

            public void call(com.isode.stroke.base.Error p1) {
                handleSessionFinished(p1);
            }
        });
        sessionNeedCredentialsConnection_ = session_.onNeedCredentials.connect(new Slot() {

            public void call() {
                handleNeedCredentials();
            }
        });
        session_.start();
    }

    private void handleConnectorFinished(final Connection connection, final com.isode.stroke.base.Error error) {
        resetConnector();
        
        if (connection == null) {
            if (options.forgetPassword) {
                purgePassword();
            }            
            ClientError clientError = null;
            if (!disconnectRequested_) {
                if (error instanceof DomainNameResolveError) {
                    clientError = new ClientError(ClientError.Type.DomainNameResolveError);
                } else {
                    clientError = new ClientError(ClientError.Type.ConnectionError);
                }
            }
            onDisconnected.emit(clientError);
        }
        else {
            assert (connection_ == null);
            assert (sessionStream_ == null);
            
            if (certificate_ != null && certificate_.isNull()) {
                // Certificate can not be read so do not initialise session
                onDisconnected.emit(new ClientError(ClientError.Type.ClientCertificateLoadError));
                return;
            }
            
            connection_ = connection;
            
            sessionStream_ = 
                    new BasicSessionStream(StreamType.ClientStreamType, connection_, payloadParserFactories_, payloadSerializers_, 
                            networkFactories.getTLSContextFactory(), networkFactories.getTimerFactory(), options.tlsOptions);
            if (certificate_ != null) {
                sessionStream_.setTLSCertificate(certificate_);
            }
            sessionStreamDataReadConnection_ = sessionStream_.onDataRead.connect(new Slot1<SafeByteArray>() {

                public void call(SafeByteArray p1) {
                    handleDataRead(p1);
                }
            });

            sessionStreamDataWrittenConnection_ = sessionStream_.onDataWritten.connect(new Slot1<SafeByteArray>() {

                public void call(SafeByteArray p1) {
                    handleDataWritten(p1);
                }
            });

            bindSessionToStream();
        }
    }



    /**
     * Close the stream and disconnect from the server.
     */
    public void disconnect() {
        // FIXME: We should be able to do without this boolean. We just have to make sure we can tell the difference between
        // connector finishing without a connection due to an error or because of a disconnect.
        disconnectRequested_ = true;
        if (session_ != null && !session_.isFinished()) {
            session_.finish();
        } else if (connector_ != null) {
            connector_.stop();
        }
    }

    /**
     * Checks whether the client is active.
     *
     * A client is active when it is connected or connecting to the server.
     */
    public boolean isActive() {
    	return (session_ != null && !session_.isFinished()) || connector_ != null;
    }

    public void setCertificate(final CertificateWithKey certificate) {
        certificate_ = certificate;
    }
    
    /**
     * Sets the certificate trust checker. If a server presents a certificate
     * which does not conform to the requirements of RFC 6120, then the
     * trust checker, if configured, will be called. If the trust checker 
     * says the certificate is trusted, then connecting will proceed; if 
     * not, the connection will end with an error.
     *
     * @param checker a CertificateTrustChecker that will be called when 
     * the server sends a TLS certificate that does not validate. 
     */
    public void setCertificateTrustChecker(final CertificateTrustChecker checker) {
        certificateTrustChecker = checker;
    }

    private void handleSessionFinished(final com.isode.stroke.base.Error error) {
        if (options.forgetPassword) {
            purgePassword();
        }
        resetSession();

        ClientError actualerror = null;
        if (error != null) {
            ClientError clientError = null;
            if (error instanceof ClientSession.Error) {
                ClientSession.Error actualError = (ClientSession.Error) error;
                switch (actualError.type) {
                    case AuthenticationFailedError:
                        clientError = new ClientError(ClientError.Type.AuthenticationFailedError);
                        break;
                    case CompressionFailedError:
                        clientError = new ClientError(ClientError.Type.CompressionFailedError);
                        break;
                    case ServerVerificationFailedError:
                        clientError = new ClientError(ClientError.Type.ServerVerificationFailedError);
                        break;
                    case NoSupportedAuthMechanismsError:
                        clientError = new ClientError(ClientError.Type.NoSupportedAuthMechanismsError);
                        break;
                    case UnexpectedElementError:
                        clientError = new ClientError(ClientError.Type.UnexpectedElementError);
                        break;
                    case ResourceBindError:
                        clientError = new ClientError(ClientError.Type.ResourceBindError);
                        break;
                    case SessionStartError:
                        clientError = new ClientError(ClientError.Type.SessionStartError);
                        break;
                    case TLSError:
                        clientError = new ClientError(ClientError.Type.TLSError);
                        break;
                    case TLSClientCertificateError:
                        clientError = new ClientError(ClientError.Type.ClientCertificateError);
                        break;
                    case StreamError:
                        clientError = new ClientError(ClientError.Type.StreamError);
                        break;
                }
            }
            else if (error instanceof TLSError) {
                TLSError actualError = (TLSError)error;
                switch(actualError.getType()) {
                    case CertificateCardRemoved:
                        clientError = new ClientError(ClientError.Type.CertificateCardRemoved);
                        break;
                    case UnknownError:
                        clientError = new ClientError(ClientError.Type.TLSError);
                        break;
                }
            }
            else if (error instanceof SessionStream.SessionStreamError) {
                SessionStream.SessionStreamError actualError = (SessionStream.SessionStreamError) error;
                switch (actualError.type) {
                    case ParseError:
                        clientError = new ClientError(ClientError.Type.XMLError);
                        break;
                    case TLSError:
                        clientError = new ClientError(ClientError.Type.TLSError);
                        break;
                    case InvalidTLSCertificateError:
                        clientError = new ClientError(ClientError.Type.ClientCertificateLoadError);
                        break;
                    case ConnectionReadError:
                        clientError = new ClientError(ClientError.Type.ConnectionReadError);
                        break;
                    case ConnectionWriteError:
                        clientError = new ClientError(ClientError.Type.ConnectionWriteError);
                        break;
                }
            } else if (error instanceof CertificateVerificationError) {
                CertificateVerificationError verificationError = (CertificateVerificationError)error;
                switch (verificationError.getType()) {
                    case UnknownError:
                        clientError = new ClientError(ClientError.Type.UnknownCertificateError);
                        break;
                    case Expired:
                        clientError = new ClientError(ClientError.Type.CertificateExpiredError);
                        break;
                    case NotYetValid:
                        clientError = new ClientError(ClientError.Type.CertificateNotYetValidError);
                        break;
                    case SelfSigned:
                        clientError = new ClientError(ClientError.Type.CertificateSelfSignedError);
                        break;
                    case Rejected:
                        clientError = new ClientError(ClientError.Type.CertificateRejectedError);
                        break;
                    case Untrusted:
                        clientError = new ClientError(ClientError.Type.CertificateUntrustedError);
                        break;
                    case InvalidPurpose:
                        clientError = new ClientError(ClientError.Type.InvalidCertificatePurposeError);
                        break;
                    case PathLengthExceeded:
                        clientError = new ClientError(ClientError.Type.CertificatePathLengthExceededError);
                        break;
                    case InvalidSignature:
                        clientError = new ClientError(ClientError.Type.InvalidCertificateSignatureError);
                        break;
                    case InvalidCA:
                        clientError = new ClientError(ClientError.Type.InvalidCAError);
                        break;
                    case InvalidServerIdentity:
                        clientError = new ClientError(ClientError.Type.InvalidServerIdentityError);
                        break;
                    case Revoked:
                        clientError = new ClientError(ClientError.Type.RevokedError);
                        break;
                    case RevocationCheckFailed:
                        clientError = new ClientError(ClientError.Type.RevocationCheckFailedError);
                        break;
                }
            }
            /* If "error" was non-null, we expect to be able to derive 
             * a non-null "clientError".
             */  
            NotNull.exceptIfNull(clientError,"clientError");
            actualerror = clientError;
        }
        onDisconnected.emit(actualerror);
    }

    private void handleNeedCredentials() {
        assert session_ != null;
        session_.sendCredentials(password_);
        if (options.forgetPassword) {
            purgePassword();
        }        
    }

    private void handleDataRead(final SafeByteArray data) {
        onDataRead.emit(data);
    }

    private void handleDataWritten(final SafeByteArray data) {
        onDataWritten.emit(data);
    }

    private void handlePresenceReceived(Presence presence) {
        onPresenceReceived.emit(presence);
    }

    private void handleMessageReceived(Message message) {
        onMessageReceived.emit(message);
    }

    private void handleStanzaAcked(Stanza stanza) {
        onStanzaAcked.emit(stanza);
    }

    private void purgePassword() {
        password_ = new SafeByteArray();
    }

    private void handleStanzaChannelAvailableChanged(final boolean available) {
        if (available) {
            iqRouter_.setJID(session_.getLocalJID());
            handleConnected();
            onConnected.emit();
        }
    }

    public void sendMessage(final Message message) {
        stanzaChannel_.sendMessage(message);
    }

    public void sendPresence(final Presence presence) {
        stanzaChannel_.sendPresence(presence);
    }

    /**
     * Sends raw, unchecked data.
     */
    public void sendData(final String data) {
        sessionStream_.writeData(data);
    }

    /**
     * Get the IQRouter responsible for all IQs on this connection.
     * Use this to send IQs.
     */
    public IQRouter getIQRouter() {
        return iqRouter_;
    }

    public StanzaChannel getStanzaChannel() {
        return stanzaChannel_;
    }

    /**
     * Checks whether the client is connected to the server,
     * and stanzas can be sent.
     * @return session is available for sending/receiving stanzas.
     */
    public boolean isAvailable() {
        return stanzaChannel_.isAvailable();
    }
    
    /**
     * Determine whether the underlying session is encrypted with TLS
     * @return true if the session is initialized and encrypted with TLS,
     * false otherwise.
     */
    public boolean isStreamEncrypted() {
        return (sessionStream_ != null && sessionStream_.isTLSEncrypted());
    }

    private void resetConnector() {
        if (connectorConnectFinishedConnection_ != null) {
            connectorConnectFinishedConnection_.disconnect();
        }
        connector_ = null;
        //TO PORT
        /*for(ConnectionFactory* f, proxyConnectionFactories) {
            delete f;
        }
        proxyConnectionFactories.clear();*/
    }
    
    protected ClientSession getSession() {
    	return session_;
    }

    protected NetworkFactories getNetworkFactories() {
        return networkFactories;
    }

    /**
     * Called before onConnected signal is emmitted.
     */
    protected void handleConnected() {}

    private void resetSession() {
        session_.onFinished.disconnectAll();
        session_.onNeedCredentials.disconnectAll();

        sessionStream_.onDataRead.disconnectAll();
        sessionStream_.onDataWritten.disconnectAll();

        if (connection_ != null) {
            connection_.disconnect();
        }
        // TO PORT
        /*else if (sessionStream_ instanceof BOSHSessionStream) {
            sessionStream_.close();
        }*/
        sessionStream_ = null;
        connection_ = null;
    }

    /**
     * @return JID of the client, will never be null. After the session was
     *         initialized, this returns the bound JID (the JID provided by
     *         the server during resource binding). Prior to this it returns 
     *         the JID provided by the user.
     */
    public JID getJID() {
        if (session_ != null) {
            return session_.getLocalJID();
        } else {
            return jid_;
        }
    }

    /**
     * Checks whether stream management is enabled.
     *
     * If stream management is enabled, onStanzaAcked will be
     * emitted when a stanza is received by the server.
     *
     * \see onStanzaAcked
     */
    public boolean getStreamManagementEnabled() {
        return stanzaChannel_.getStreamManagementEnabled();
    }

    private void forceReset() {
        if (connector_ != null) {
            logger_.warning("Client not disconnected properly: Connector still active\n");
            resetConnector();
        }
        if (sessionStream_ != null || connection_ != null) {
            logger_.warning("Client not disconnected properly: Session still active\n");
            resetSession();
        }

    }
    
    @Override
    public String toString()
    {
        return "CoreClient for \"" + jid_ + "\"" +
        "; session " + (isAvailable() ? "" : "un") + "available"; 
    }

}
