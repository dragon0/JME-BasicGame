package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.input.controls.ActionListener;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication implements AnimEventListener {

    private AnimChannel channel;
    private AnimControl animControl;
    private Node playerStart;
    private Node player;
    private ChaseCamera chaseCam;
    private boolean movingForward;
    private boolean movingBackward;
    private boolean movingLeft;
    private boolean movingRight;
    private BulletAppState bulletAppState;
    private CharacterControl playerCharacterControl;
    // frame variables
    float moveBy;
    boolean moving;
    float ccRot;
    float rotation;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private Node scene;

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.LightGray);
        initPhysics();
        initKeys();
        initScene();
        initPlayer();
        initCamera();
//        initLight();
//        initDebugItems();
    }

    @Override
    public void simpleUpdate(float tpf) {
        beginFrame(tpf);
        checkMovement();
        move();
    }

    private void beginFrame(float tpf) {
        moveBy = 5 * tpf;
        moving = false;
        ccRot = chaseCam.getHorizontalRotation();
        rotation = -ccRot - FastMath.PI / 2;
    }

    private void checkMovement() {
        if ((movingForward && movingBackward) || (movingLeft && movingRight)) {
            // invalid states; do nothing
        }
        else if (movingForward && movingLeft) {
            rotation += FastMath.PI / 4;
            moving = true;
        }
        else if (movingForward && movingRight) {
            rotation -= FastMath.PI / 4;
            moving = true;
        }
        else if (movingBackward && movingLeft) {
            rotation -= FastMath.PI + (FastMath.PI / 4);
            moving = true;
        }
        else if (movingBackward && movingRight) {
            rotation += FastMath.PI + (FastMath.PI / 4);
            moving = true;
        }
        else if (movingForward) {
            moving = true;
        }
        else if (movingBackward) {
            rotation -= FastMath.PI;
            moving = true;
        }
        else if (movingRight) {
            rotation -= FastMath.PI / 2;
            moving = true;
        }
        else if (movingLeft) {
            rotation += FastMath.PI / 2;
            moving = true;
        }
    }

    private void move() {
        if (moving) {
//            setPlayerTransform(); // old version, replaced by below
            Vector3f trans = new Vector3f(
                    moveBy * FastMath.sin(rotation),
                    0,
                    moveBy * FastMath.cos(rotation));
            playerCharacterControl.setWalkDirection(trans);
            playerCharacterControl.setViewDirection(trans);
        }
        else {
            playerCharacterControl.setWalkDirection(Vector3f.ZERO);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    private void initKeys() {
        inputManager.addMapping("Jump",
                new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("moveForward",
                new KeyTrigger(KeyInput.KEY_UP),
                new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("moveBackward",
                new KeyTrigger(KeyInput.KEY_DOWN),
                new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("moveRight",
                new KeyTrigger(KeyInput.KEY_RIGHT),
                new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("moveLeft",
                new KeyTrigger(KeyInput.KEY_LEFT),
                new KeyTrigger(KeyInput.KEY_A));
        inputManager.addListener(analogListener,
                "moveForward", "moveBackward", "moveRight", "moveLeft");
        inputManager.addListener(actionListener, "Jump");
    }
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("Jump") && !isPressed) {
                if (!channel.getAnimationName().equals("Jump")) {
                    channel.setAnim("Jump", 0.5f);
                    channel.setLoopMode(LoopMode.Loop);
                    playerCharacterControl.jump();
                }
            }
        }
    };
    private ActionListener analogListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("moveForward")) {
                movingForward = isPressed;
            }
            if (name.equals("moveBackward")) {
                movingBackward = isPressed;
            }
            if (name.equals("moveRight")) {
                movingRight = isPressed;
            }
            if (name.equals("moveLeft")) {
                movingLeft = isPressed;
            }

            if ((movingBackward || movingForward || movingLeft || movingRight)
                    && !((movingForward && movingBackward) || (movingLeft && movingRight))) {
                if (channel.getAnimationName().equals("Stand")) {
                    channel.setAnim("Walk", 0.5f);
                    channel.setLoopMode(LoopMode.Loop);
                }
            }
            else {
                channel.setAnim("Stand", 0.5f);
                channel.setLoopMode(LoopMode.DontLoop);
            }
        }
    };

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Jump")) {
            if (moving) {
                channel.setAnim("Walk", 0.5f);
                channel.setLoopMode(LoopMode.Loop);
            }
            else {
                channel.setAnim("Stand", 0.5f);
                channel.setLoopMode(LoopMode.DontLoop);
                channel.setSpeed(1f);
            }
        }
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        //unused
    }

    private void initLight() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -1f, -1f).normalizeLocal());
        rootNode.addLight(dl);
    }

    private void initPlayer() {
        Node p = (Node) assetManager.loadModel("Models/simple_walk_1.j3o");
//        p.setLocalScale(0.5f);
//        rootNode.attachChild(p);
        player = (Node) p.getChild("Cube");
        scene.attachChild(player);
        player.setLocalTransform(playerStart.getLocalTransform());

        animControl = player.getControl(AnimControl.class);
        animControl.addListener(this);
        channel = animControl.createChannel();
        channel.setAnim("Stand");

        CapsuleCollisionShape cap = new CapsuleCollisionShape(0.5f, 1f, 1);
        playerCharacterControl = new CharacterControl(cap, 0.05f);
        playerCharacterControl.setJumpSpeed(20);
        playerCharacterControl.setFallSpeed(30);
        playerCharacterControl.setGravity(30);
        playerCharacterControl.setPhysicsLocation(player.getLocalTranslation());
        Quaternion q = player.getLocalRotation();
        Vector3f trans = q.getRotationColumn(2);
        playerCharacterControl.setViewDirection(trans);

        player.addControl(playerCharacterControl);
        bulletAppState.getPhysicsSpace().add(playerCharacterControl);
    }

    private void initCamera() {
        // Disable the default flyby cam
        flyCam.setEnabled(false);
        stateManager.detach(stateManager.getState(FlyCamAppState.class));

        // Enable a chase cam for this target (typically the player).
        chaseCam = new ChaseCamera(cam, player, inputManager);
        chaseCam.setSmoothMotion(true);
        chaseCam.setDefaultDistance(10);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setRotationSpeed(3);
        chaseCam.setDragToRotate(false);

        // Ensure the camera starts behind the player
        Quaternion q = player.getLocalRotation();
        float cameraInitAngle = q.toAngles(null)[1];
        cameraInitAngle = -cameraInitAngle - FastMath.PI / 2;
        chaseCam.setDefaultHorizontalRotation(cameraInitAngle);
    }

    private void initScene() {
        scene = (Node) assetManager.loadModel("Scenes/scene1.j3o");
//        p.setLocalScale(0.5f);
        rootNode.attachChild(scene);
        playerStart = (Node) scene.getChild("PlayerStart");
        //TODO check if jme will load a scene with linked datablocks
        walkNodes(scene);
    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
//        bulletAppState.setDebugEnabled(true);
    }

    private void walkNodes(Node p) {
        Queue<Spatial> q = new ArrayDeque();
        q.add(p);
        while (!q.isEmpty()) {
            Spatial s = q.remove();
            if (s instanceof Node) {
                Node n = (Node) s;
                q.addAll(n.getChildren());
            }

            System.out.println(s.getName());

            if (hasKey(s, "clip")) {
                int d = s.getUserData("clip");
                if (d != 0) {
                    int mass = 0;
                    if (hasKey(s, "mass")) {
                        mass = s.getUserData("mass");
                    }
                    applyPhysics(s, mass);
                }
            }
        }
    }

    private boolean hasKey(Spatial s, String key) {
        return s.getUserData(key) != null;
    }

    private void applyPhysics(Spatial s, int mass) {
        CollisionShape sceneShape =
                CollisionShapeFactory.createMeshShape((Node) s);
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, mass);
        s.addControl(landscape);
        bulletAppState.getPhysicsSpace().add(landscape);
    }

    private void setPlayerTransform() {
        // This method moves the player model manually.
        // It has been replaced with the physics-based code in move().
        Transform t = player.getLocalTransform();
        Quaternion q = new Quaternion();
        q.fromAngleAxis(rotation, new Vector3f(0, 1, 0));
        t.setRotation(q);


        Quaternion q1 = t.getRotation();
        float playerRot = q1.toAngleAxis(new Vector3f(0, 1, 0));
        System.out.printf("cam: %f, player: %f\n", ccRot, playerRot);

        Vector3f trans = new Vector3f(
                moveBy * FastMath.sin(rotation),
                0,
                moveBy * FastMath.cos(rotation));
        /*
         | cos θ    0   sin θ| |x|   | x cos θ + z sin θ|   |x'|
         |   0      1       0| |y| = |         y        | = |y'|
         |−sin θ    0   cos θ| |z|   |−x sin θ + z cos θ|   |z'|
         */
        t.setTranslation(t.getTranslation().add(trans));

        player.setLocalTransform(t);
    }

    //////////////////////////////////////////////////
    // This stuff is for debugging and can be safely removed later
    private void initDebugItems() {
        makeBox(-2, 0, 1);
        makeBox(2, 0, 2);
        makeBox(-2, 0, -1);
    }

    private void makeBox(int x, int y, int z) {
        /* A colored lit cube. Needs light source! */
        Box boxMesh = new Box(1f, 1f, 1f);
        Geometry boxGeo = new Geometry("Colored Box", boxMesh);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        boxMat.setBoolean("UseMaterialColors", true);
        boxMat.setColor("Ambient", ColorRGBA.Green);
        boxMat.setColor("Diffuse", ColorRGBA.Green);
        boxGeo.setMaterial(boxMat);
        boxGeo.setLocalTranslation(x, y, z);
        rootNode.attachChild(boxGeo);
    }
}
