package dataStructure;

public class S_LinkedList_Int
{
    class Node
    {
        int val;
        Node next;
        Node(int val)
        {
            this.val=val;
            next=null;
        }
    }
    Node first = null;
    public void insertAtLast(int y)
    {
        Node n = new Node(y);
        if(first==null)
        {
            first=n;
        }
        else
        {
            Node temp=first;
            while (temp.next!=null)
            {
                temp=temp.next;
            }
            temp.next=n;
        }
    }
    public int findByPosition(int pos) {
        if (first == null) {
            System.out.println("List is empty");
            return 0;
        }

        if (pos <= 0) {
            //System.out.println("Invalid position (must be >= 1)");
            return 0;
        }

        Node temp = first;
        int index = 1;

        while (temp != null) {
            if (index == pos) {
                System.out.println("Value at position " + pos + " is: " + temp.val);
                return temp.val;
            }
            temp = temp.next;
            index++;
        }

        System.out.println("Position " + pos + " does not exist in the list");
        return 0;
    }
    public boolean isEmpty()
    {
        if (first==null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    public int size()
    {
        if (first==null)
        {
            return 0;
        }
        else
        {
            int s=0;
            Node temp = first;
            while (temp!=null)
            {
                s=s+1;
                temp=temp.next;
            }
            return s;
        }
    }
}


