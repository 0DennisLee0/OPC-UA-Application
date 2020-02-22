package server;

import methods.GenerateEventMethod;
import methods.SqrtMethod;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.DataTypeEncodingNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import types.CustomDataType;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

//管理命名空间

public class CncNamespace extends ManagedNamespace {

    static final String NAMESPACE_URI = "urn:eclipse:milo:cnc";

    //region Values
    private static final Object[][] STATIC_SCALAR_NODES = new Object[][]{
            {"Boolean", Identifiers.Boolean, new Variant(false)},
            {"Byte", Identifiers.Byte, new Variant(ubyte(0x00))},
            {"SByte", Identifiers.SByte, new Variant((byte) 0x00)},
            {"Integer", Identifiers.Integer, new Variant(32)},
            {"Int16", Identifiers.Int16, new Variant((short) 16)},
            {"Int32", Identifiers.Int32, new Variant(32)},
            {"Int64", Identifiers.Int64, new Variant(64L)},
            {"UInteger", Identifiers.UInteger, new Variant(uint(32))},
            {"UInt16", Identifiers.UInt16, new Variant(ushort(16))},
            {"UInt32", Identifiers.UInt32, new Variant(uint(32))},
            {"UInt64", Identifiers.UInt64, new Variant(ulong(64L))},
            {"Float", Identifiers.Float, new Variant(3.14f)},
            {"Double", Identifiers.Double, new Variant(3.14d)},
            {"String", Identifiers.String, new Variant("string value")},
            {"DateTime", Identifiers.DateTime, new Variant(DateTime.now())},
            {"Guid", Identifiers.Guid, new Variant(UUID.randomUUID())},
            {"ByteString", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
            {"XmlElement", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
            {"LocalizedText", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
            {"QualifiedName", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
            {"NodeId", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))},
            {"Variant", Identifiers.BaseDataType, new Variant(32)},
            {"Duration", Identifiers.Duration, new Variant(1.0)},
            {"UtcTime", Identifiers.UtcTime, new Variant(DateTime.now())},
    };

    private static final Object[][] STATIC_ARRAY_NODES = new Object[][]{
            {"BooleanArray", Identifiers.Boolean, false},
            {"ByteArray", Identifiers.Byte, ubyte(0)},
            {"SByteArray", Identifiers.SByte, (byte) 0x00},
            {"Int16Array", Identifiers.Int16, (short) 16},
            {"Int32Array", Identifiers.Int32, 32},
            {"Int64Array", Identifiers.Int64, 64L},
            {"UInt16Array", Identifiers.UInt16, ushort(16)},
            {"UInt32Array", Identifiers.UInt32, uint(32)},
            {"UInt64Array", Identifiers.UInt64, ulong(64L)},
            {"FloatArray", Identifiers.Float, 3.14f},
            {"DoubleArray", Identifiers.Double, 3.14d},
            {"StringArray", Identifiers.String, "string value"},
            {"DateTimeArray", Identifiers.DateTime, DateTime.now()},
            {"GuidArray", Identifiers.Guid, UUID.randomUUID()},
            {"ByteStringArray", Identifiers.ByteString, new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04})},
            {"XmlElementArray", Identifiers.XmlElement, new XmlElement("<a>hello</a>")},
            {"LocalizedTextArray", Identifiers.LocalizedText, LocalizedText.english("localized text")},
            {"QualifiedNameArray", Identifiers.QualifiedName, new QualifiedName(1234, "defg")},
            {"NodeIdArray", Identifiers.NodeId, new NodeId(1234, "abcd")}
    };
    //endregion

    //记录日志
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Random random = new Random();

    //创建订阅模型
    private final SubscriptionModel subscriptionModel;

    //在指定的命名空间URI中创建server
    CncNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);
    }

    @Override
    protected void onStartup() {
        //注册该命名空间
        super.onStartup();

        // Create a "HelloWorld" folder and add it to the node manager
        //创建一个folder，来容纳子节点
        NodeId folderNodeId = newNodeId("cnc");

        //定义结点的属性
        UaFolderNode folderNode = new UaFolderNode(
                getNodeContext(),
                folderNodeId,
                //BrowseName是HelloWorld
                newQualifiedName("CNC"),
                //在英国地区注册，名称是HelloWorld
                LocalizedText.english("CNC")
        );

        //获取当下的结点管理器，并添加结点
        getNodeManager().addNode(folderNode);

        //添加引用，由Objects来Organize该folder结点
        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.Organizes,
                //引用指向
                Identifiers.ObjectsFolder.expanded(),
                //表明箭头指向，地址空间的箭头
                //反着的，因为是Object来Organize folder
                false
        ));

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("CNC/" + "Axis"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName("Axis"))
                .setDisplayName(LocalizedText.english("Axis"))
                .setDataType(Identifiers.Int32)
                //通过Identifier来表示
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        Variant variant = new Variant(uint(10));
        node.setValue(new DataValue(variant));
        //生成值的时候呼叫委托，---详见：委托模式
        node.setAttributeDelegate(new ValueLoggingDelegate());

        getNodeManager().addNode(node);
        folderNode.addOrganizes(node);

        //region Set the EventNotifier bit on Server Node for Events.
        UaNode serverNode = getServer()
                .getAddressSpaceManager()
                .getManagedNode(Identifiers.Server)
                .orElse(null);

        if (serverNode instanceof ServerNode) {
            ((ServerNode) serverNode).setEventNotifier(ubyte(1));

            // Post a bogus Event every couple seconds
            getServer().getScheduledExecutorService().scheduleAtFixedRate(() -> {
                try {
                    BaseEventNode eventNode = getServer().getEventFactory().createEvent(
                            newNodeId(UUID.randomUUID()),
                            Identifiers.BaseEventType
                    );

                    eventNode.setBrowseName(new QualifiedName(1, "foo"));
                    eventNode.setDisplayName(LocalizedText.english("foo"));
                    eventNode.setEventId(ByteString.of(new byte[]{0, 1, 2, 3}));
                    eventNode.setEventType(Identifiers.BaseEventType);
                    eventNode.setSourceNode(serverNode.getNodeId());
                    eventNode.setSourceName(serverNode.getDisplayName().getText());
                    eventNode.setTime(DateTime.now());
                    eventNode.setReceiveTime(DateTime.NULL_VALUE);
                    eventNode.setMessage(LocalizedText.english("event message!"));
                    eventNode.setSeverity(ushort(2));

                    getServer().getEventBus().post(eventNode);

                    eventNode.delete();
                } catch (Throwable e) {
                    logger.error("Error creating EventNode: {}", e.getMessage(), e);
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
        //endregion
    }

    //region 只添加一个文件夹结点,以下可以暂时忽略
    /**
     * 在根节点下添加对应的CNC定义结点，Demo版
     *
     * @param rootFolder 文件夹结点
     */
    private void addCncObjectTypeAndInstance(UaFolderNode rootFolder) {
        /*
        1.添加机床的类型结点
         */
        UaObjectTypeNode cncTypeNode = UaObjectTypeNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/CNCType"))
                .setBrowseName(newQualifiedName("CNCType"))
                .setDisplayName(LocalizedText.english("CNCType"))
                .setIsAbstract(false)
                .build();


        UaVariableNode cncName = UaVariableNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/CNCType.Name"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_ONLY)))
                .setBrowseName(newQualifiedName("Name"))
                .setDisplayName(LocalizedText.english("Name"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();


        //要求添加必须强制命名的机床类型
        cncName.addReference(new Reference(
                cncName.getNodeId(),
                Identifiers.HasModellingRule,
                Identifiers.ModellingRule_Mandatory.expanded(),
                true
        ));

        cncName.setValue(new DataValue(new Variant("")));
        //名称是Axis
        cncTypeNode.addComponent(cncName);

        UaVariableNode axisValue = UaVariableNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/CNCType.AxisValue"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName("AxisValue"))
                .setDisplayName(LocalizedText.english("AxisValue"))
                .setDataType(Identifiers.Double)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        axisValue.addReference(new Reference(
                axisValue.getNodeId(),
                Identifiers.HasModellingRule,
                Identifiers.ModellingRule_Mandatory.expanded(),
                true
        ));

        //定义完轴数据
        axisValue.setValue(new DataValue(new Variant(0.0)));
        cncTypeNode.addComponent(axisValue);

        //注册Typenode到server里
        getServer().getObjectTypeManager().registerObjectType(
                cncTypeNode.getNodeId(),
                UaObjectNode.class,
                UaObjectNode::new
        );

        // Add the inverse SubtypeOf relationship.
        cncTypeNode.addReference(new Reference(
                cncTypeNode.getNodeId(),
                Identifiers.HasSubtype,
                Identifiers.BaseObjectType.expanded(),
                false
        ));
        //将定义的类型结点添加到NodeManager里
        getNodeManager().addNode(cncTypeNode);
        getNodeManager().addNode(cncName);
        getNodeManager().addNode(axisValue);

        //创建一个cncNode实例
        try {
            UaObjectNode cnc = (UaObjectNode) getNodeFactory().createNode(
                    newNodeId("HelloWorld/CNC"),
                    cncTypeNode.getNodeId(),
                    false
            );
            cnc.setBrowseName(newQualifiedName("CNC"));
            cnc.setDisplayName(LocalizedText.english("MyCNC"));

            //在根目录里添加cnc结点
            rootFolder.addOrganizes(cnc);
            cnc.addReference(new Reference(
                    cnc.getNodeId(),
                    Identifiers.Organizes,
                    rootFolder.getNodeId().expanded(),
                    false
            ));
        } catch (UaException e) {
            logger.error("Error creating cnc instance: {}", e.getMessage(), e);
        }
    }

    private void addCncDataTypeVariable(UaFolderNode rootFolder){
        NodeId cncDataTypeId = newNodeId("DataType.CncDataType");

        UaDataTypeNode cncDataTypeNode = new UaDataTypeNode(
                getNodeContext(),
                cncDataTypeId,
                newQualifiedName("CncDataType"),
                LocalizedText.english("CncDataType"),
                LocalizedText.english("CncDataType"),
                uint(0),
                uint(0),
                false
        );

        getNodeManager().addNode(cncDataTypeNode);
        cncDataTypeNode.addReference(new Reference(
                cncDataTypeId,
                Identifiers.HasSubtype,
                Identifiers.Structure.expanded(),
                false
        ));

        // Forward ref from Structure
        Optional<UaDataTypeNode> structureDataTypeNode = getNodeManager()
                .getNode(Identifiers.Structure)
                .map(UaDataTypeNode.class::cast);

        structureDataTypeNode.ifPresent(node ->
                node.addReference(new Reference(
                        node.getNodeId(),
                        Identifiers.HasSubtype,
                        cncDataTypeId.expanded(),
                        true
                ))
        );


    }

    private void addVariableNodes(UaFolderNode rootNode) {
        addArrayNodes(rootNode);
        addScalarNodes(rootNode);
        addAdminReadableNodes(rootNode);
        addAdminWritableNodes(rootNode);
        addDynamicNodes(rootNode);
        addDataAccessNodes(rootNode);
        addWriteOnlyNodes(rootNode);
    }

    private void addArrayNodes(UaFolderNode rootNode) {
        UaFolderNode arrayTypesFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/ArrayTypes"),
                newQualifiedName("ArrayTypes"),
                LocalizedText.english("ArrayTypes")
        );

        getNodeManager().addNode(arrayTypesFolder);
        //指向这个Folder结点
        rootNode.addOrganizes(arrayTypesFolder);

        for (Object[] os : STATIC_ARRAY_NODES) {
            //根据对应的结点类型添加其名字，类型，值等
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            //可以存放一个数组,值的类型不同，所以用Object
            Object value = os[2];

            //根据类型和长度创建一个新的类数组
            Object array = Array.newInstance(value.getClass(), 5);

            //添加5个位置的,存放同一个value
            for (int i = 0; i < 5; i++) {
                //对应的数组值
                Array.set(array, i, value);
            }

            //添加一个新的数组Variant（变体）
            Variant variant = new Variant(array);

            //getNodeContext -- 获取结点的上下文环境
            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    //前面是命名空间（ArrayFolder），最后是名称（数组Folder+变量名称）
                    //在默认的Namespace中，这里引用了ManagedNamespace，即是最开始的
                    //static final String NAMESPACE_URI = "urn:eclipse:milo:hello-world";
                    .setNodeId(newNodeId("HelloWorld/ArrayTypes/" + name))
                    //获取可写的级别
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    //获取用户的操作权限
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    //获取浏览名称
                    .setBrowseName(newQualifiedName(name))
                    //获取展示的名称
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    //设置类型的定义
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    //设置数组的阶数（维数），一位数组，二维数组
                    .setValueRank(ValueRank.OneDimension.getValue())
                    //设置数组的长度
                    .setArrayDimensions(new UInteger[]{uint(0)})
                    .build();

            //为结点赋值 --- 变体的值
            //Variant variant = new Variant(array)，这个变量是一个Array
            node.setValue(new DataValue(variant));

            //将值的变化委托给结点，里面可以传递结点的父节点
            node.setAttributeDelegate(new ValueLoggingDelegate());
            //将该数组结点添加至Node Manager
            getNodeManager().addNode(node);
            //添加Organize引用
            arrayTypesFolder.addOrganizes(node);
        }
    }

    private void addScalarNodes(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/ScalarTypes"),
                newQualifiedName("ScalarTypes"),
                LocalizedText.english("ScalarTypes")
        );

        getNodeManager().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        for (Object[] os : STATIC_SCALAR_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            //第三个标志就是他的对应值
            Variant variant = (Variant) os[2];

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("HelloWorld/ScalarTypes/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    //通过Identifier来表示
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));
            //生成值的时候呼叫委托，---详见：委托模式
            node.setAttributeDelegate(new ValueLoggingDelegate());

            getNodeManager().addNode(node);
            scalarTypesFolder.addOrganizes(node);
        }
    }

    private void addWriteOnlyNodes(UaFolderNode rootNode) {
        UaFolderNode writeOnlyFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/WriteOnly"),
                newQualifiedName("WriteOnly"),
                LocalizedText.english("WriteOnly")
        );

        getNodeManager().addNode(writeOnlyFolder);
        rootNode.addOrganizes(writeOnlyFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/WriteOnly/" + name))
                //AccessLevel设置成Write_Only
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.WRITE_ONLY)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.WRITE_ONLY)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        //添加一个字段用来描述其不可读的性质
        node.setValue(new DataValue(new Variant("can't read this")));

        getNodeManager().addNode(node);
        writeOnlyFolder.addOrganizes(node);
    }

    private void addAdminReadableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/OnlyAdminCanRead"),
                newQualifiedName("OnlyAdminCanRead"),
                LocalizedText.english("OnlyAdminCanRead")
        );

        getNodeManager().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/OnlyAdminCanRead/" + name))
                //没有setUserAccessLevel方法对User进行授权
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant("shh... don't tell the users")));

        //添加限制访问委托
        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.NONE;
            }
        }));

        getNodeManager().addNode(node);
        adminFolder.addOrganizes(node);
    }

    private void addAdminWritableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/OnlyAdminCanWrite"),
                newQualifiedName("OnlyAdminCanWrite"),
                LocalizedText.english("OnlyAdminCanWrite")
        );

        getNodeManager().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/OnlyAdminCanWrite/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant("admin was here")));

        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.READ_ONLY;
            }
        }));

        getNodeManager().addNode(node);
        adminFolder.addOrganizes(node);
    }

    private void addDynamicNodes(UaFolderNode rootNode) {
        UaFolderNode dynamicFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/Dynamic"),
                newQualifiedName("Dynamic"),
                LocalizedText.english("Dynamic")
        );

        getNodeManager().addNode(dynamicFolder);
        rootNode.addOrganizes(dynamicFolder);

        // Dynamic Boolean
        {
            String name = "Boolean";
            NodeId typeId = Identifiers.Boolean;
            Variant variant = new Variant(false);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("HelloWorld/Dynamic/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                    new AttributeDelegate() {
                        @Override
                        public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                            //返回一个随机赋值的Boolean
                            return new DataValue(new Variant(random.nextBoolean()));
                        }
                    },
                    //就是 new ValueLoggingDelegate，没有输出，返回一个ValueLoggingDelegate
                    ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);
        }

        // Dynamic Int32
        {
            String name = "Int32";
            NodeId typeId = Identifiers.Int32;
            Variant variant = new Variant(0);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("HelloWorld/Dynamic/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                    new AttributeDelegate() {
                        @Override
                        public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                            return new DataValue(new Variant(random.nextInt()));
                        }
                    },
                    ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);
        }

        // Dynamic Double
        {
            String name = "Double";
            NodeId typeId = Identifiers.Double;
            Variant variant = new Variant(0.0);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                    .setNodeId(newNodeId("HelloWorld/Dynamic/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                    new AttributeDelegate() {
                        @Override
                        public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                            return new DataValue(new Variant(random.nextDouble()));
                        }
                    },
                    ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);
        }
    }

    private void addDataAccessNodes(UaFolderNode rootNode) {
        // DataAccess folder
        UaFolderNode dataAccessFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("HelloWorld/DataAccess"),
                newQualifiedName("DataAccess"),
                LocalizedText.english("DataAccess")
        );

        getNodeManager().addNode(dataAccessFolder);
        rootNode.addOrganizes(dataAccessFolder);

        //带单位的量
        // AnalogItemType node
        try {
            //getNodeFactory节点的工厂类
            AnalogItemNode node = (AnalogItemNode) getNodeFactory().createNode(
                    newNodeId("HelloWorld/DataAccess/AnalogValue"),
                    Identifiers.AnalogItemType,
                    true
            );

            node.setBrowseName(newQualifiedName("AnalogValue"));
            node.setDisplayName(LocalizedText.english("AnalogValue"));
            node.setDataType(Identifiers.Double);
            node.setValue(new DataValue(new Variant(3.14d)));

            //设定范围
            node.setEURange(new Range(0.0, 100.0));

            getNodeManager().addNode(node);
            dataAccessFolder.addOrganizes(node);
        } catch (UaException e) {
            logger.error("Error creating AnalogItemType instance: {}", e.getMessage(), e);
        }
    }

    //添加节点方法
    private void addSqrtMethod(UaFolderNode folderNode) {
        UaMethodNode methodNode = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/sqrt(x)"))
                .setBrowseName(newQualifiedName("sqrt(x)"))
                .setDisplayName(new LocalizedText(null, "sqrt(x)"))
                .setDescription(
                        //添加方法的说明
                        LocalizedText.english("Returns the correctly rounded positive square root of a double value."))
                .build();

        SqrtMethod sqrtMethod = new SqrtMethod(methodNode);
        //设置Property，以及获取参数的方法
        methodNode.setProperty(UaMethodNode.InputArguments, sqrtMethod.getInputArguments());
        methodNode.setProperty(UaMethodNode.OutputArguments, sqrtMethod.getOutputArguments());
        //设置函数的处理方式
        methodNode.setInvocationHandler(sqrtMethod);

        getNodeManager().addNode(methodNode);

        //添加对根节点的引用
        methodNode.addReference(new Reference(
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                false
        ));
    }

    //设置产生事件的方法
    private void addGenerateEventMethod(UaFolderNode folderNode) {
        UaMethodNode methodNode = UaMethodNode.builder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/generateEvent(eventTypeId)"))
                .setBrowseName(newQualifiedName("generateEvent(eventTypeId)"))
                .setDisplayName(new LocalizedText(null, "generateEvent(eventTypeId)"))
                .setDescription(
                        LocalizedText.english("Generate an Event with the TypeDefinition indicated by eventTypeId."))
                .build();

        GenerateEventMethod generateEventMethod = new GenerateEventMethod(methodNode);
        methodNode.setProperty(UaMethodNode.InputArguments, generateEventMethod.getInputArguments());
        methodNode.setProperty(UaMethodNode.OutputArguments, generateEventMethod.getOutputArguments());
        methodNode.setInvocationHandler(generateEventMethod);

        getNodeManager().addNode(methodNode);

        //添加一个对于根节点的引用
        methodNode.addReference(new Reference(
                //可以在Reference里面自定义相应的ReferenceType
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                false
        ));
    }

    private void addCustomObjectTypeAndInstance(UaFolderNode rootFolder) {
        // Define a new ObjectType called "MyObjectType".
        //到时候可以通过一个叠类来生成
        UaObjectTypeNode objectTypeNode = UaObjectTypeNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/MyObjectType"))
                .setBrowseName(newQualifiedName("MyObjectType"))
                .setDisplayName(LocalizedText.english("MyObjectType"))
                .setIsAbstract(false)
                .build();

        // "Foo" and "Bar" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode foo = UaVariableNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/MyObjectType.Foo"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName("Foo"))
                .setDisplayName(LocalizedText.english("Foo"))
                .setDataType(Identifiers.Int16)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();


        foo.setValue(new DataValue(new Variant(0)));
        //通过对类型添加addComponent约束，表明foo是自定义对象类型的member
        objectTypeNode.addComponent(foo);

        UaVariableNode bar = UaVariableNode.builder(getNodeContext())
                .setNodeId(newNodeId("ObjectTypes/MyObjectType.Bar"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName("Bar"))
                .setDisplayName(LocalizedText.english("Bar"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        bar.addReference(new Reference(
                bar.getNodeId(),
                Identifiers.HasModellingRule,
                Identifiers.ModellingRule_Mandatory.expanded(),
                true
        ));

        bar.setValue(new DataValue(new Variant("bar")));
        objectTypeNode.addComponent(bar);

        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        getServer().getObjectTypeManager().registerObjectType(
                objectTypeNode.getNodeId(),
                UaObjectNode.class,
                UaObjectNode::new
        );

        // Add the inverse SubtypeOf relationship.
        objectTypeNode.addReference(new Reference(
                objectTypeNode.getNodeId(),
                Identifiers.HasSubtype,
                Identifiers.BaseObjectType.expanded(),
                false
        ));

        // Add type definition and declarations to address space.
        getNodeManager().addNode(objectTypeNode);
        getNodeManager().addNode(foo);
        getNodeManager().addNode(bar);

        // Use NodeFactory to create instance of MyObjectType called "MyObject".
        // NodeFactory takes care of recursively instantiating MyObject member nodes
        // as well as adding all nodes to the address space.
        try {
            UaObjectNode myObject = (UaObjectNode) getNodeFactory().createNode(
                    newNodeId("HelloWorld/MyObject"),
                    //获取ObjectType的Node ID
                    objectTypeNode.getNodeId(),
                    //表明全部是强制结点
                    false
            );
            //设置结点的名称
            myObject.setBrowseName(newQualifiedName("MyObject"));
            myObject.setDisplayName(LocalizedText.english("MyObject"));

            // Add forward and inverse references from the root folder.
            rootFolder.addOrganizes(myObject);

            //添加一个反向的指针指向rootFolder
            myObject.addReference(new Reference(
                    myObject.getNodeId(),
                    Identifiers.Organizes,
                    rootFolder.getNodeId().expanded(),
                    false
            ));
        } catch (UaException e) {
            logger.error("Error creating MyObjectType instance: {}", e.getMessage(), e);
        }
    }

    //都是添加到根节点下面
    private void addCustomDataTypeVariable(UaFolderNode rootFolder) {
        //datatypeId表示自定义节点的类型ID
        // add a custom DataTypeNode as a subtype of the built-in Structure DataTypeNode
        NodeId dataTypeId = newNodeId("DataType.CustomDataType");

        UaDataTypeNode dataTypeNode = new UaDataTypeNode(
                getNodeContext(),
                dataTypeId,
                newQualifiedName("CustomDataType"),
                LocalizedText.english("CustomDataType"),
                LocalizedText.english("CustomDataType"),
                uint(0),
                uint(0),
                false
        );

        getNodeManager().addNode(dataTypeNode);

        // Inverse ref to Structure
        //这是Structure的一个子类
        dataTypeNode.addReference(new Reference(
                dataTypeId,
                Identifiers.HasSubtype,
                Identifiers.Structure.expanded(),
                false
        ));

        // Forward ref from Structure，添加一个structure的Type定义
        Optional<UaDataTypeNode> structureDataTypeNode = getNodeManager()
                .getNode(Identifiers.Structure)
                //对于其下的子节点，把UaDataTypeNode转换成structure下面的节点对应的类型
                .map(UaDataTypeNode.class::cast);

        structureDataTypeNode.ifPresent(node ->
                node.addReference(new Reference(
                        node.getNodeId(),
                        Identifiers.HasSubtype,
                        dataTypeId.expanded(),
                        true
                ))
        );

        // TODO this should probably get a node and a HasEncoding reference from dataTypeNode...
        NodeId binaryEncodingId = newNodeId("DataType.CustomDataType.BinaryEncoding");

        //二进制编码节点
        DataTypeEncodingNode binaryEncodingNode = new DataTypeEncodingNode(
                getNodeContext(),
                binaryEncodingId,
                newQualifiedName("BinaryEncoding"),
                LocalizedText.english("BinaryEncoding"),
                LocalizedText.english("BinaryEncoding"),
                uint(0),
                uint(0)
        );

        binaryEncodingNode.addReference(new Reference(
                binaryEncodingNode.getNodeId(),
                Identifiers.HasEncoding,
                dataTypeId.expanded(),
                false
        ));

        getNodeManager().addNode(binaryEncodingNode);

        // Register codec with the server DataTypeManager instance
        getServer().getDataTypeManager().registerCodec(
                binaryEncodingId,
                //传递一个二进制编码解码器
                new CustomDataType.Codec().asBinaryCodec()
        );

        //定仪的值类型实例
        UaVariableNode customDataTypeVariable = UaVariableNode.builder(getNodeContext())
                .setNodeId(newNodeId("HelloWorld/CustomDataTypeVariable"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(newQualifiedName("CustomDataTypeVariable"))
                .setDisplayName(LocalizedText.english("CustomDataTypeVariable"))
                .setDataType(dataTypeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        getNodeManager().addNode(customDataTypeVariable);

        CustomDataType value = new CustomDataType(
                "foo",
                uint(42),
                true
        );

        ExtensionObject xo = ExtensionObject.encodeDefaultBinary(
                getServer().getSerializationContext(),
                value,
                binaryEncodingId
        );

        //以二进制码的方式获取数据
        customDataTypeVariable.setValue(new DataValue(new Variant(xo)));

        rootFolder.addOrganizes(customDataTypeVariable);

        customDataTypeVariable.addReference(new Reference(
                customDataTypeVariable.getNodeId(),
                Identifiers.Organizes,
                rootFolder.getNodeId().expanded(),
                false
        ));
    }
    //endregion


    //监听节点行为的方法
    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

}
