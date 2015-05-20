import numpy as np
import json
import matplotlib.pyplot as plt

f = list(open('LT29i-1.txt'))
f = [json.loads(x.strip('\n')) for x in f]

acc = np.array([json.loads(x['acc']) for x in f])
ori = np.array([json.loads(x['orientation']) for x in f])
omega = np.array([json.loads(x['omega']) for x in f])
tm = np.array([x['time'] for x in f]).astype(float) / 1E9
tm -= tm[0]

K = 100

# plt.plot(tm[:K], acc[:K])
# plt.hist(acc)
# plt.hist(ori[:,1])
# plt.hist(omega)
# plt.show()

def stat(x):
    mean = np.mean(x, axis=0)
    std = np.std(x, axis=0)
    print(mean, std)

print(len(tm))

stat(acc)
stat(ori)
stat(omega)

acc -= np.mean(acc, axis=0)

dt = np.diff(tm)
dt = np.concatenate(([0], dt))

vel = np.cumsum(acc * dt.reshape((dt.shape[0], 1)), axis=0)

stat(vel)
plt.plot(tm, vel)
plt.show()
