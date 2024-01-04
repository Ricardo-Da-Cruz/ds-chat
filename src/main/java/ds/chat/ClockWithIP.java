package ds.chat;

class ClockWithIP implements Comparable<ClockWithIP>{
    private final int clock;
    private final int ip;

    ClockWithIP(int clock, byte[] ip) {
        this.clock = clock;
        this.ip = convertByteArrayToInt(ip);
    }

    public int convertByteArrayToInt(byte[] ipAddress) {
        int result = 0;

        for (byte b : ipAddress) {
            result = (result << 8) | (b & 0xFF);
        }

        return result;
    }

    @Override
    public int compareTo(ClockWithIP o) {
        if (clock == o.clock){
            return ip - o.ip;
        }
        return clock - o.clock;
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof ClockWithIP){
            ClockWithIP other = (ClockWithIP) o;
            return clock == other.clock && ip == other.ip;
        }
        return false;
    }
}
