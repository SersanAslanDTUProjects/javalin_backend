import database.dao.Controller;
import io.javalin.Javalin;
import resources.GalgelegResource;
import resources.UserAdminResource;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

public class Main {
    public static Javalin app;


    public static void main(String[] args) throws Exception {
        start();

    }

    public static void stop() {
        app.stop();
        app = null;
    }

    public static void start() throws Exception {
        if (app != null) return;

        app = Javalin.create(config -> {
            config.enableCorsForAllOrigins()
                    .addStaticFiles("webapp");
        }).start(8088);

        /*                    .addSinglePageRoot("/", "webapp/index.html");
         */

        app.before(ctx -> {
            System.out.println("Javalin Server fik " + ctx.method() + " på " + ctx.url() + " med query " + ctx.queryParamMap() + " og form " + ctx.formParamMap());
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
        });


        // REST endpoints
        app.get("/rest/hej", ctx -> ctx.result("Hejsa, godt at møde dig!"));
        app.get("/rest/hej/:fornavn", ctx -> ctx.result("Hej " + ctx.queryParam("fornavn") + ", godt at møde dig!"));

        app.get("rest/galgeleg/highscore", ctx ->
                ctx.json(GalgelegResource.getHighscoreListe()).contentType("json"));
        app.post("rest/galgeleg/:username", ctx ->
                ctx.result(GalgelegResource.startGame(ctx.pathParam("username"))).contentType("json"));
        app.get("rest/galgeleg/:username/:guess", ctx ->
                ctx.result(GalgelegResource.makeGuess(ctx.pathParam("username"), ctx.pathParam("guess"))).contentType("json"));

        //NJL - er i brug
        app.get("rest/playground_list", ctx ->
                ctx.json(Controller.getInstance().getPlaygrounds()).contentType("json"));
        app.post("rest/user_login", ctx ->
                ctx.json(UserAdminResource.verifyLogin(ctx.body(), ctx)).contentType("json"));
        app.post("rest/create_user", ctx ->
                ctx.json(UserAdminResource.createUser(ctx.body(), ctx)).contentType("json"));
        app.put("rest/update_user", ctx ->
                ctx.json(UserAdminResource.updateUser(ctx.body(), ctx)).contentType("json"));
        app.get("rest/user_list", ctx ->
                ctx.json(Controller.getInstance().getUsers()).contentType("json"));
        app.post("rest/remove_user", ctx ->
                ctx.json(UserAdminResource.deleteUser(ctx.body(), ctx)).contentType("json"));



    }

}
