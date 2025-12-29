package uv.simuladoraleexplorador.main.utils;

import javax.vecmath.Quat4f;

public class MathUtils {

    // Convierte Grados (Euler X,Y,Z) a Cuaternión (JBullet)
    public static Quat4f eulerToQuaternion(double xDeg, double yDeg, double zDeg) {
        // Convertir a radianes
        double pitch = Math.toRadians(xDeg);
        double yaw   = Math.toRadians(yDeg);
        double roll  = Math.toRadians(zDeg);

        Quat4f q = new Quat4f();
        // Lógica matemática para crear cuaternión desde Euler
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        q.w = (float) (cr * cp * cy + sr * sp * sy);
        q.x = (float) (sr * cp * cy - cr * sp * sy);
        q.y = (float) (cr * sp * cy + sr * cp * sy);
        q.z = (float) (cr * cp * sy - sr * sp * cy);
        return q;
    }

    // Convierte un Cuaternión a un array [X, Y, Z] en grados (Para guardar)
    public static double[] quaternionToEuler(Quat4f q) {
        double[] angles = new double[3];

        // Roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        angles[0] = Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp));

        // Pitch (y-axis rotation)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        if (Math.abs(sinp) >= 1)
            angles[1] = Math.toDegrees(Math.copySign(Math.PI / 2, sinp)); // use 90 degrees if out of range
        else
            angles[1] = Math.toDegrees(Math.asin(sinp));

        // Yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        angles[2] = Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp));

        return angles;
    }
}